package net.domaincentric.scheduling.application.eventsourcing

import java.time.Instant
import java.util.UUID

import monix.eval.Task

trait EventHandler {
  def handle(
      event: Any,
      metadata: EventMetadata,
      eventId: UUID,
      position: Version,
      occurredAt: Instant
  ): Task[Unit]
}
