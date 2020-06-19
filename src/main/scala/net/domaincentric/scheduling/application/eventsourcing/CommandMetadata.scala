package net.domaincentric.scheduling.application.eventsourcing

case class CommandMetadata(correlationId: String, causationId: String)
