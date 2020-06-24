package net.domaincentric.scheduling.application.eventsourcing

import java.time.Instant
import java.util.UUID

case class MessageEnvelope[M](
    data: Any,
    metadata: M,
    eventId: UUID,
    version: Version,
    occurredAt: Instant,
    streamPosition: Option[Version]
)
