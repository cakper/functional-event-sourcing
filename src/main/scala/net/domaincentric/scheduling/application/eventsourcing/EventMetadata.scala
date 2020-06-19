package net.domaincentric.scheduling.application.eventsourcing

case class EventMetadata(correlationId: String, causationId: String, replayed: Boolean = false)
