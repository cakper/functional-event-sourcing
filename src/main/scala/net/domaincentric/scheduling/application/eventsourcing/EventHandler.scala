package net.domaincentric.scheduling.application.eventsourcing

import java.time.Instant
import java.util.UUID

import monix.eval.Task
import net.domaincentric.scheduling.eventsourcing.Event

trait EventHandler {
  def handle[E <: Event](
      event: E,
      metadata: EventMetadata,
      eventId: UUID,
      position: Version,
      occurredAt: Instant
  ): Task[Unit]
}
