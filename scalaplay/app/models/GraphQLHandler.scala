package models

import play.api.mvc._
import play.api.libs.json._
import scala.util.Try

case class GraphQLComponents(query: String, operationName: Option[String], variables: Option[JsObject])

case object GraphQLHandler {
	def parseBodyToGraphQLQuery(json: JsValue): Try[GraphQLComponents] = {
    val extract: JsValue => GraphQLComponents = query =>
      GraphQLComponents(
        (query \ "query").as[String],
        (query \ "operationName").asOpt[String],
        (query \ "variables").toOption.flatMap {
          case JsString(vars) => Some(parseVariables(vars))
          case obj: JsObject => Some(obj)
          case _ => None
        }
      )
    Try {
      json match {
        case arrayBody@JsArray(_) => extract(arrayBody.value(0))
        case objectBody@JsObject(_) => extract(objectBody)
        case otherType =>
          throw new Error {
            s"/graphql endpoint does not support request body of type [${otherType.getClass.getSimpleName}]"
          }
      }
    }
	}

	private def parseVariables(variables: String) =
		if (variables.trim == "" || variables.trim == "null") Json.obj() else Json.parse(variables).as[JsObject]
}