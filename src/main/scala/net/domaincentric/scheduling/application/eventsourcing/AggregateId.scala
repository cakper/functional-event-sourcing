package net.domaincentric.scheduling.application.eventsourcing

case class AggregateId(value: String) {
  require(value.nonEmpty, "AggregateId can't be empty")
  // TODO: Add other requirements such as non printable chars
}
