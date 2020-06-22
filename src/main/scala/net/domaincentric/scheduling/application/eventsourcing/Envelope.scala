package net.domaincentric.scheduling.application.eventsourcing

import java.time.Instant
import java.util.UUID

case class Envelope[M](
    data: Any,
    metadata: M,
    eventId: UUID,
    version: Version,
    occurredAt: Instant
)
