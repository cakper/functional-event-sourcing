package net.domaincentric.scheduling.application.eventsourcing

case class AggregateId(value: String, `type:`: String) {
  override def toString: String = s"${`type:`}-$value"
}
