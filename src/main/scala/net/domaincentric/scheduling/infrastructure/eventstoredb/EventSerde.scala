package net.domaincentric.scheduling.infrastructure.eventstoredb

import java.util.UUID

import com.eventstore.dbclient.{ ProposedEvent, ResolvedEvent }
import io.circe._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.{ MessageEnvelope, EventMetadata }
import net.domaincentric.scheduling.domain.aggregate.doctorday._
import net.domaincentric.scheduling.infrastructure.circe.Implicits._

import scala.util.Try

class EventSerde extends Serde[EventMetadata] {
  private val prefix = "doctorday"

  def deserialize(resolvedEvent: ResolvedEvent): Try[MessageEnvelope[EventMetadata]] = Try {
    val rawEvent = resolvedEvent.getEvent
    val event = (rawEvent.getEventType match {
      case s"$prefix-day-scheduled"          => decode[DayScheduled] _
      case s"$prefix-slot-scheduled"         => decode[SlotScheduled] _
      case s"$prefix-slot-booked"            => decode[SlotBooked] _
      case s"$prefix-slot-booking-cancelled" => decode[SlotBookingCancelled] _
    })(new String(rawEvent.getEventData)).toOption.get

    val metadata = decode[EventMetadata](new String(rawEvent.getUserMetadata)).toOption.get
    eventsourcing.MessageEnvelope(
      event,
      metadata,
      rawEvent.getEventId,
      rawEvent.getStreamRevision.getValueUnsigned,
      rawEvent.getCreated,
      Option(resolvedEvent.getLink).map(_.getStreamRevision.getValueUnsigned)
    )
  }

  def serialize(event: Any, metadata: EventMetadata): Try[ProposedEvent] = Try {
    event match {
      case e: DayScheduled         => toProposedEvent(s"$prefix-day-scheduled", e.asJson, metadata)
      case e: SlotScheduled        => toProposedEvent(s"$prefix-slot-scheduled", e.asJson, metadata)
      case e: SlotBooked           => toProposedEvent(s"$prefix-slot-booked", e.asJson, metadata)
      case e: SlotBookingCancelled => toProposedEvent(s"$prefix-slot-booking-cancelled", e.asJson, metadata)
      case c: CancelSlotBooking =>
        toProposedEvent(s"$prefix-command-cancel-slot-booking", c.asJson, metadata)
    }
  }

  private def toProposedEvent(eventType: String, data: Json, metadata: EventMetadata) = {
    new ProposedEvent(
      UUID.randomUUID(),
      eventType,
      "application/json",
      data.noSpaces.getBytes,
      metadata.asJson.dropNullValues.noSpaces.getBytes
    )
  }
}
