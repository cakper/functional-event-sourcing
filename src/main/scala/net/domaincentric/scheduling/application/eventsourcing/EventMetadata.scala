package net.domaincentric.scheduling.application.eventsourcing

case class EventMetadata(correlationId: CorrelationId, causationId: CausationId, replayed: Option[String] = None)
