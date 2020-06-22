package net.domaincentric.scheduling.application.eventsourcing

case class CommandMetadata(correlationId: CorrelationId, causationId: CausationId, aggregateId: AggregateId)
