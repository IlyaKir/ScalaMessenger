package models.events

import models.Message

case class MessageEvent(name: String, message: Message) extends Event{
  val userId: Long = message.userId
  val eventName: String = name
}