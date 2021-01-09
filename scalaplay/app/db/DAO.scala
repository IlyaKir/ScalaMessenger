package db

import models._
import slick.jdbc.PostgresProfile.api._
import DBSchema._
import scala.concurrent.Future
import java.time.Instant

import errors._

class DAO(db: Database) {

	// USERS
	def allUsers(drop: Option[Long], take: Option[Long]): Future[Seq[User]] = {
		// TODO create method for table drop and take
		val dropQuery = users.drop(drop getOrElse(0L))
		val dropTakeQuery = take match {
			case Some(value) => dropQuery.take(value)
			case None => dropQuery
		}
		db.run(dropTakeQuery.result)
	}

	def tryGetUserId(userName: String): Future[Option[Long]] = db.run(
		users.filter(_.name === userName).map(_.userId).result.headOption
	)

	def getUser(userId: Long): Future[Option[User]] = db.run(
		users.filter(_.userId === userId).result.headOption
	)

	def createUser(name: String): Future[User] = {
		val insertAndReturnUserQuery = (users returning users.map(_.userId)) into {
			(user, id) => user.copy(userId = id)
		}
		db.run {
			insertAndReturnUserQuery += User(0, name)
		}
	}

	// MESSAGES
	def allMessages(drop: Option[Long], take: Option[Long]): Future[Seq[Message]] = {
		// TODO create method for table drop and take
		val dropQuery = messages.drop(drop getOrElse(0L))
		val dropTakeQuery = take match {
				case Some(value) => dropQuery.take(value)
				case None => dropQuery
		}
		db.run(dropTakeQuery.result)
	}

	def getUserMessages(userId: Long, substring: Option[String], drop: Option[Long], take: Option[Long]): Future[Seq[Message]] = {
		// TODO try find case insensitive 'like' ?
		val substrQuery = messages
			.filter(_.userId === userId)
			.filterOpt(substring)((row, substr) => {
				row.message.toLowerCase like s"%${substr.toLowerCase()}%"
			})
		val substrDropQuery = substrQuery.drop(drop getOrElse(0L))
		val resultQuery = take match {
			case Some(value) => substrDropQuery.take(value)
			case None => substrDropQuery
		}
		db.run(resultQuery.result)
	}

	def createMessage(requestUserId: Long, message: String) = {
		val insertAndReturnMessage = (messages returning messages.map(_.messageId)) into {
			(message, id) => message.copy(messageId = id)
		}
		db.run {
			insertAndReturnMessage += Message(0, message, requestUserId, Instant.now().toString())
		}
		//(messages.returning(messages.map(_.messageId))) += Message(0, message, requestUserId, Instant.now().toString())
	}

	// TODO     :Future[Message] 
	def updateMessage(requestUserId: Long, messageId: Long, message: String) = db.run(
		messages.filter(_.messageId === messageId).result.headOption flatMap {
			case Some(row) if row.userId == requestUserId =>
				val update = messages.filter(_.messageId === messageId).map(_.message).update(message)
				val getMessage = messages.filter(_.messageId === messageId).result.head
				update.andThen(getMessage)
			case Some(row) => DBIO.failed(WrongHeaderException)
			case None => DBIO.failed(WrongMessageIdException)
		}
	)

	// TODO     :Future[Int]
	def deleteMessage(requestUserId: Long, messageId: Long) = db.run(
		messages.filter(_.messageId === messageId).result.headOption flatMap {
			case Some(row) if row.userId == requestUserId => 
				messages.filter(_.messageId === messageId).delete
			case Some(row) => DBIO.failed(WrongHeaderException)
			case None => DBIO.failed(WrongMessageIdException)
		}
	)
}