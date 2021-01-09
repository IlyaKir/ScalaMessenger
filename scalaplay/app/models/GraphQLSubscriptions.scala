package models

import monix.execution.Cancelable
import play.api.Logger

import scala.collection.mutable.ArrayBuffer

/**
  * A class which contains a list of subscriptions which was opened
  * during one WebSocket connection by a user and which can be canceled on demand.
  */
case class GraphQLSubscriptions(){

  private[this] val subscriptions: ArrayBuffer[Cancelable] = ArrayBuffer.empty[Cancelable]
  private var closed = false

  def add(cancelable: Cancelable): Unit = this.synchronized {
    if (!closed) {
      cancelable +: subscriptions
    }
  }

  def cancelAll(): Unit = this.synchronized {
    subscriptions.foreach(_.cancel())
    subscriptions.clear()
    closed = true
  }

  def subscriptionsCount: Int = {
    this.subscriptions.size
  }
}