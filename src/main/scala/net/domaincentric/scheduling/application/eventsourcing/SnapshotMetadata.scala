package net.domaincentric.scheduling.application.eventsourcing

case class SnapshotMetadata(correlationId: CorrelationId, causationId: CausationId, version: Version)
