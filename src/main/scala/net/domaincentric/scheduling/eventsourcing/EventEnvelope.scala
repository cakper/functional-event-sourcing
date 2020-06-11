package net.domaincentric.scheduling.eventsourcing

import java.time.Instant
import java.util.UUID

case class EventEnvelope(event: Event, metadata: EventMetadata, eventId: UUID, position: Version, occurredAt: Instant)
