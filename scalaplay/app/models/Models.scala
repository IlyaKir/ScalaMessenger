package models

case class User(userId: Long, name: String)
case class Message(messageId: Long, message: String, userId: Long, createdAt: String)