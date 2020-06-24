package net.domaincentric.scheduling.application.eventsourcing

case class SubscriptionId(value: String) {
  override def toString: String = value
}
