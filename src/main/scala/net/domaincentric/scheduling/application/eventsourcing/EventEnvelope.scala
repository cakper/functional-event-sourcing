package net.domaincentric.scheduling.application.eventsourcing

import java.time.Instant
import java.util.UUID

import net.domaincentric.scheduling.eventsourcing.Event

case class EventEnvelope(
    event: Event,
    metadata: EventMetadata,
    eventId: UUID,
    position: Version,
    occurredAt: Instant
)
