package net.domaincentric.scheduling.infrastructure.eventstoredb

import scala.concurrent.duration._

case class SubscriptionOptions(connectionTimeout: FiniteDuration, bufferCapacity: Int)

object SubscriptionOptions {
  val default: SubscriptionOptions = SubscriptionOptions(10.seconds, 4096)
}
