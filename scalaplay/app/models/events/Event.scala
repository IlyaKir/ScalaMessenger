package models.events

trait Event {
  def userId: Long
  def eventName: String
}