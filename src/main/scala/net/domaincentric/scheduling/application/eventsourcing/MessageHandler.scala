package net.domaincentric.scheduling.application.eventsourcing

import java.time.Instant
import java.util.UUID

import monix.eval.Task

trait MessageHandler[M] {
  def handle(envelope: MessageEnvelope[M]): Task[Unit] = handle(
    envelope.data,
    envelope.metadata,
    envelope.eventId,
    envelope.version,
    envelope.occurredAt,
    envelope.streamPosition
  )

  def handle(
      message: Any,
      metadata: M,
      messageId: UUID,
      position: Version,
      occurredAt: Instant,
      streamPosition: Option[Version]
  ): Task[Unit]
}
