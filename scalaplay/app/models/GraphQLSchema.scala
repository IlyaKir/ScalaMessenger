package models

import akka.stream.Materializer
import models.events._
import sangria.schema._
import sangria.macros.derive._
import sangria.streaming.akkaStreams._
import services.PubSubService

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class GraphQLSchema @Inject()(pubSubService: PubSubService[MessageEvent])
  (implicit ec: ExecutionContext, mat: Materializer){

  val UserType = deriveObjectType[Unit, User]()
  val MessageType = deriveObjectType[Unit, Message]()

  val userId = Argument("userId", LongType)
  val name = Argument("name", StringType)
  val substring = Argument("substring", OptionInputType(StringType))
  val message = Argument("message", StringType)
  val messageId = Argument("messageId", LongType)
  val subscribeEvents = Argument("events", ListInputType(StringType))

  val drop = Argument("drop", OptionInputType(LongType))
  val take = Argument("take", OptionInputType(LongType))

  val QueryType = ObjectType[MyContext, Unit](
    "Query",
    fields[MyContext, Unit](
      Field("allUsers",
        fieldType = ListType(UserType),
        arguments = List(drop, take),
        resolve = c => c.ctx.secured((dao, headerUserId) => dao.allUsers(c.arg(drop), c.arg(take)))),
      Field("allMessages",
        fieldType = ListType(MessageType),
        arguments = List(drop, take),
        resolve = c => c.ctx.secured((dao, headerUserId) => dao.allMessages(c.arg(drop), c.arg(take)))),
      Field("user",
        fieldType = OptionType(UserType),
        arguments = List(userId),
        resolve = c => c.ctx.secured((dao, headerUserId) => dao.getUser(c.arg(userId)))),
      Field("userMessages",
        fieldType = ListType(MessageType),
        arguments = List(userId, substring, drop, take), 
        resolve = c => c.ctx.secured((dao, headerUserId) => dao.getUserMessages(c.arg(userId), c.arg(substring), c.arg(drop), c.arg(take))))
    )
  )

  val MutationType = ObjectType[MyContext, Unit](
    "Mutation",
    fields[MyContext, Unit](
      Field("createUser",
        fieldType = UserType,
        arguments = List(name),
        resolve = c => c.ctx.unsecured(dao => dao.createUser(c.arg(name)))),
      Field("createMessage",
        fieldType = MessageType,
        arguments = List(message),
        resolve = c => c.ctx.secured((dao, headerUserId) => dao.createMessage(headerUserId, c.arg(message)))
          .map {
            createdMessage => pubSubService.publish(MessageEvent("createMessage", createdMessage))
            createdMessage 
          }),
      Field("updateMessage",
        fieldType = MessageType,
        arguments = List(messageId, message),
        resolve = c => c.ctx.secured((dao, headerUserId) => dao.updateMessage(headerUserId, c.arg(messageId), c.arg(message)))
          .map {
            updateMessage => pubSubService.publish(MessageEvent("updateMessage", updateMessage))
            updateMessage
          }),
      Field("deleteMessage",
        fieldType = IntType,
        arguments = List(messageId),
        resolve = c => c.ctx.secured((dao, headerUserId) => dao.deleteMessage(headerUserId, c.arg(messageId))))
    )
  )

  val MessageEventType = deriveObjectType[Unit, MessageEvent](
    ReplaceField("message", Field("message", MessageType, resolve = c => c.value.message)))

  val Subscriptions = ObjectType[MyContext, Unit](
    "Subscription",
    fields[MyContext, Unit](
      Field.subs(
        name = "messageUpdated",
        fieldType = MessageEventType,
        arguments = List(userId, subscribeEvents),
        resolve = c => pubSubService.subscribe(c.arg(userId), c.arg(subscribeEvents))(c.ctx)
      )
    )
  )

  val SchemaDefinition = Schema(QueryType, Some(MutationType), Some(Subscriptions))
}