package controllers

import db.DBSchema
import models._
import models.GraphQLHandler._
import errors._
import actors.WebSocketFlowActor

import javax.inject.Inject
import play.api.mvc._
import play.api.libs.json._
import akka.actor._
import play.api.libs.streams.ActorFlow

import sangria.execution._
import sangria.parser.{QueryParser, SyntaxError}
import sangria.marshalling.playJson._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext
import akka.stream.Materializer

class Application @Inject() (graphQL: GraphQLSchema)(implicit ec: ExecutionContext, as: ActorSystem, mat: Materializer) extends InjectedController {
	private val dao = DBSchema.createDatabase

	def graphqlSubscriptionOverWebSocket: WebSocket = WebSocket.accept[String, String] { _ =>
		ActorFlow.actorRef(client => WebSocketFlowActor.props(graphQL, dao, client))
	}

	def graphqlRequest: Action[JsValue] = Action.async(parse.json) { implicit request =>
		val headers: Headers = request.headers
		val userName: Option[String] = headers.get("user-name")
		parseBodyToGraphQLQuery(request.body) match {
			case Success(body) => executeQuery(userName, body.query, body.variables, body.operationName)
			case Failure(error) => Future.successful(BadRequest(error.getMessage()))
		}
	}

	private def executeQuery(userName: Option[String], query: String, variables: Option[JsObject], operation: Option[String]) =
		QueryParser.parse(query) match {
			case Success(queryAst) =>
				Executor.execute(
					graphQL.SchemaDefinition,
					queryAst,
					MyContext(userName, dao),
					operationName = operation,
					exceptionHandler = exceptionHandler,
					variables = variables getOrElse Json.obj())
				.map(Ok(_))
				.recover {
					case error: QueryAnalysisError => BadRequest(error.resolveError)
					case error: ErrorWithResolver => InternalServerError(error.resolveError)
				}

			case Failure(error: SyntaxError) =>
				Future.successful(BadRequest(Json.obj(
				"syntaxError" -> error.getMessage,
				"locations" -> Json.arr(Json.obj(
					"line" -> error.originalError.position.line,
					"column" -> error.originalError.position.column)))))

			case Failure(error) =>
				throw error
		}

	lazy val exceptionHandler = ExceptionHandler {
		case (_, error @ NoHeaderException) => HandledException(error.getMessage)
		case (_, error @ WrongHeaderException) => HandledException(error.getMessage)
		case (_, error @ WrongMessageIdException) => HandledException(error.getMessage)
		case (_, error @ MaxQueryDepthReachedError(_)) => HandledException(error.getMessage)
	}
}