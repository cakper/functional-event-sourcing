package net.domaincentric.scheduling.eventsourcing

case class EventMetadata(correlationId: String, causationId: String, position: Long)
