package models

import db._
import errors._

import scala.concurrent.Future

case class MyContext(userName: Option[String], private val dao: DAO, graphQlSubs: Option[GraphQLSubscriptions] = None){
  implicit val ec = scala.concurrent.ExecutionContext.global

	def secured[T](f: (DAO, Long) => Future[T]): Future[T] = {
		userName match {
			case Some(value) => {
				dao.tryGetUserId(value) flatMap {
					case Some(userId) => f(dao, userId)
					case None => Future.failed(WrongHeaderException)
				}
			}
			case None => Future.failed(NoHeaderException)
		}
	}

	def unsecured[T](f: DAO => Future[T]): Future[T] = {
		f(dao)
	}
}