package db

import slick.jdbc.PostgresProfile.api._

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps

import models._
import slick.jdbc.meta.MTable

object DBSchema {
  implicit val ec = scala.concurrent.ExecutionContext.global

  def createDatabase: DAO = {
    val db = Database.forConfig("postgresql")

    val dbSetup = db.run(MTable.getTables.flatMap(v => {
      val names = v.map(mt => mt.name.name)
      val createIfNotExist = tables.filter(table => 
        (!names.contains(table.baseTableRow.tableName))).map(_.schema.create)
      DBIO.sequence(createIfNotExist)
    }))

    Await.result(dbSetup, 
      Duration.Inf)

    new DAO(db)
  }

  class UsersTable(tag: Tag) extends Table[User](tag, "users"){
    def userId = column[Long]("userId", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.Unique)

    def * = (userId, name).mapTo[User]
  }

  val users = TableQuery[UsersTable]

  class MessageTable(tag: Tag) extends Table[Message](tag, "messages"){
    def messageId = column[Long]("messageId", O.PrimaryKey, O.AutoInc)
    def message = column[String]("message")
    def userId = column[Long]("userId")
    def createdAt = column[String]("createdAt")

    def * = (messageId, message, userId, createdAt).mapTo[Message]

    def userId_fk = foreignKey("userId_fk", userId, users)(_.userId, onDelete = ForeignKeyAction.Cascade)
  }

  val messages = TableQuery[MessageTable]

  val tables = List(users, messages)
}