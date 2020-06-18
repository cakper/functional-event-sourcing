package net.domaincentric.scheduling.application.eventsourcing

import java.time.Instant
import java.util.UUID

case class EventEnvelope(
    event: Any,
    metadata: EventMetadata,
    eventId: UUID,
    version: Version,
    occurredAt: Instant
)
