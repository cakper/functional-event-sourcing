package net.domaincentric.scheduling.infrastructure.eventstoredb

import java.util.UUID

import com.eventstore.dbclient.{ ProposedEvent, ResolvedEvent }
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.{ Checkpoint, CommandMetadata, MessageEnvelope, Version }
import net.domaincentric.scheduling.domain.aggregate.doctorday.CancelSlotBooking

import scala.util.Try

class CheckpointSerde extends Serde[Unit] {
  override def serialize(data: Any, metadata: Unit): Try[ProposedEvent] = Try {
    data match {
      case c: Checkpoint =>
        new ProposedEvent(
          UUID.randomUUID(),
          "checkpoint",
          "application/json",
          c.asJson.noSpaces.getBytes,
          new Array[Byte](0)
        )
    }
  }

  override def deserialize(resolvedEvent: ResolvedEvent): Try[MessageEnvelope[Unit]] = Try {
    val raw   = resolvedEvent.getEvent
    val event = decode[Checkpoint](new String(raw.getEventData)).toOption.get

    eventsourcing.MessageEnvelope(
      event,
      (),
      raw.getEventId,
      raw.getStreamRevision.getValueUnsigned,
      raw.getCreated,
      Option(resolvedEvent.getLink).map(_.getStreamRevision.getValueUnsigned)
    )
  }
}
