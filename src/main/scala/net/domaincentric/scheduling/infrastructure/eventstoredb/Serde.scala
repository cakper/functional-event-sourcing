package net.domaincentric.scheduling.infrastructure.eventstoredb

import java.util.UUID

import com.eventstore.dbclient.{ ProposedEvent, ResolvedEvent }
import io.circe.Json
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.{ EventMetadata, MessageEnvelope }

import scala.util.Try

trait Serde[M] {
  def serialize(data: Any, metadata: M): Try[ProposedEvent]
  def deserialize(resolvedEvent: ResolvedEvent): Try[MessageEnvelope[M]]

  protected def toProposedEvent(eventType: String, data: Json, metadata: Json): ProposedEvent = {
    new ProposedEvent(
      UUID.randomUUID(),
      eventType,
      "application/json",
      data.noSpaces.getBytes,
      metadata.dropNullValues.noSpaces.getBytes
    )
  }

  protected def toEnvelope[M](
      event: Any,
      metadata: M,
      resolvedEvent: ResolvedEvent,
  ): MessageEnvelope[M] = {
    eventsourcing.MessageEnvelope(
      event,
      metadata,
      resolvedEvent.getEvent.getEventId,
      resolvedEvent.getEvent.getStreamRevision.getValueUnsigned,
      resolvedEvent.getEvent.getCreated,
      Option(resolvedEvent.getLink).map(_.getStreamRevision.getValueUnsigned)
    )
  }
}
