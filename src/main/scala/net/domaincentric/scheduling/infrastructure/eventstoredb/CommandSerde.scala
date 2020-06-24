package net.domaincentric.scheduling.infrastructure.eventstoredb

import java.util.UUID

import com.eventstore.dbclient.{ ProposedEvent, ResolvedEvent }
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.{ CommandMetadata, MessageEnvelope, Version }
import net.domaincentric.scheduling.domain.aggregate.doctorday.CancelSlotBooking

import scala.util.Try

class CommandSerde extends Serde[CommandMetadata] {
  private val prefix = "doctorday-command"
  override def serialize(data: Any, metadata: CommandMetadata): Try[ProposedEvent] = Try {
    data match {
      case c: CancelSlotBooking => toProposedEvent(s"$prefix-cancel-slot-booking", c.asJson, metadata)
    }
  }
  private def toProposedEvent(`type`: String, data: Json, metadata: CommandMetadata) = {
    new ProposedEvent(
      UUID.randomUUID(),
      `type`,
      "application/json",
      data.noSpaces.getBytes,
      metadata.asJson.noSpaces.getBytes
    )
  }

  override def deserialize(resolvedEvent: ResolvedEvent): Try[MessageEnvelope[CommandMetadata]] = Try {
    val raw = resolvedEvent.getEvent
    val event = (raw.getEventType match {
      case s"$prefix-cancel-slot-booking" => decode[CancelSlotBooking] _
    })(new String(raw.getEventData)).toOption.get

    val metadata = decode[CommandMetadata](new String(raw.getUserMetadata)).toOption.get
    eventsourcing.MessageEnvelope(
      event,
      metadata,
      raw.getEventId,
      raw.getStreamRevision.getValueUnsigned,
      raw.getCreated,
      Option(resolvedEvent.getLink).map(_.getStreamRevision.getValueUnsigned)
    )
  }
}
