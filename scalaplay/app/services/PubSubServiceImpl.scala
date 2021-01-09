package services

import akka.NotUsed
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import models.MyContext
import models.events.Event
import monix.execution.Scheduler
import monix.reactive.subjects.PublishSubject
import play.api.Logger
import sangria.schema.Action

class PubSubServiceImpl[T <: Event](implicit val scheduler: Scheduler) extends PubSubService[T] {
  private val subject: PublishSubject[T] = PublishSubject[T]
  private val bufferSize = 42

  override def publish(event: T): Unit = {
    subject.onNext(event)
  }

  override def subscribe(userId: Long, eventNames: Seq[String])
                        (implicit myContext: MyContext): Source[Action[Nothing, T], NotUsed] = {
    require(eventNames.nonEmpty)
    Source
      .actorRef[T](bufferSize, OverflowStrategy.dropHead)
      .mapMaterializedValue {
        actorRef =>
          myContext.graphQlSubs.foreach {
            subs =>
              val cancelable = subject.subscribe(new ActorRefObserver[T](actorRef))
              subs.add(cancelable)
          }
          NotUsed
      }
      .filter {
        event => (event.userId == userId) && eventNames.contains(event.eventName)
      }
      .map {
        event =>
          Action(event)
      }
  }
}