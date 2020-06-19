package net.domaincentric.scheduling.application.eventsourcing

case class CommandEnvelope(aggregateId: String, command: Any, metadata: CommandMetadata)
