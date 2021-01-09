package actors

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import models.{GraphQLSchema, GraphQLSubscriptions}
import models.GraphQLHandler._
import play.api.libs.json._
import sangria.ast.Document
import sangria.ast.OperationType.Subscription
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.playJson._
import sangria.parser.QueryParser
import sangria.streaming.akkaStreams.AkkaSource

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import db.DAO
import models.MyContext
import models.GraphQLComponents

object WebSocketFlowActor {
  def props(graphQL: GraphQLSchema, dao: DAO, client: ActorRef)
            (implicit ec: ExecutionContext, mat: Materializer): Props = {
    Props(new WebSocketFlowActor(graphQL, dao, client, GraphQLSubscriptions()))
  }
}

// The class WebSocketFlowActor is designed to process all the messages
// that the client application sends to our Scala server via WebSockets.
case class WebSocketFlowActor(graphQL: GraphQLSchema,
                              dao: DAO,
                              client: ActorRef,
                              graphQLSubscriptions: GraphQLSubscriptions)
                              (implicit ec: ExecutionContext, mat: Materializer) extends Actor {
  override def postStop(): Unit = {
    graphQLSubscriptions.cancelAll()
  }

  // for accepting GraphQL queries of the String type sent by clients over WebSockets.
  override def receive: Receive = {
    case message: String =>
      val maybeQuery: Try[GraphQLComponents] = Try(Json.parse(message)) match {
        case Success(json) => parseBodyToGraphQLQuery(json)
        case Failure(error) => throw new Error(s"Fail to parse a request body. Reason [$error]")
      }

      // TODO как сообщения попадают в AkkaSource ?
      val source: AkkaSource[JsValue] = maybeQuery match {
        // returns a new source — an executed GraphQL subscription
        case Success(body) => executeQuery(body.query, body.variables, body.operationName)
        case Failure(error) => Source.single(JsString(error.getMessage))
      }
      // respond to the subscription query that came through a WebSocket connection.
      source.map(_.toString).runWith(Sink.actorRef[String](client, onCompleteMessage = PoisonPill))
      client ! ("subscribed")
  }
  
  def executeQuery(query: String, variables: Option[JsObject] = None, operation: Option[String] = None)
                  (implicit mat: Materializer): AkkaSource[JsValue] = {
    QueryParser.parse(query) match {
      case Success(queryAst: Document) =>
        queryAst.operationType(operation) match {
          case Some(Subscription) =>
            import sangria.execution.ExecutionScheme.Stream
            import sangria.streaming.akkaStreams._

            Executor.execute(
              schema = graphQL.SchemaDefinition,
              queryAst = queryAst,
              variables = variables.getOrElse(Json.obj()),
              userContext = MyContext(None, dao, Some(graphQLSubscriptions))
            ).recover {
              case error: QueryAnalysisError => Json.obj("BadRequest" -> error.resolveError)
              case error: ErrorWithResolver => Json.obj("InternalServerError" -> error.resolveError)
            }

          case _ => Source.single {
            Json.obj("UnsupportedType" -> JsString(s"$operation"))
          }
        }

        case Failure(ex) => Source.single {
          Json.obj("BadRequest" -> JsString(s"${ex.getMessage}"))
        }
    }
  }
}