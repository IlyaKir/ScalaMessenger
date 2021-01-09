package services

import akka.NotUsed
import akka.stream.scaladsl.Source
import models.MyContext
import sangria.schema.Action

/**
  * A service to publish or subscribe to events
  *
  * @tparam T an entity which is published
  */
trait PubSubService[T] {
  def publish(event: T): Unit
  def subscribe(userId: Long, eventNames: Seq[String])(implicit myContext: MyContext): Source[Action[Nothing, T], NotUsed]
}