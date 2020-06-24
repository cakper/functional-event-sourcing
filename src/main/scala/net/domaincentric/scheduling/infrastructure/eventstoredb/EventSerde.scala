package net.domaincentric.scheduling.infrastructure.eventstoredb

import com.eventstore.dbclient.{ ProposedEvent, ResolvedEvent }
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import net.domaincentric.scheduling.application.eventsourcing.{ EventMetadata, MessageEnvelope }
import net.domaincentric.scheduling.domain.aggregate.doctorday._
import net.domaincentric.scheduling.infrastructure.circe.Implicits._

import scala.util.Try

class EventSerde extends Serde[EventMetadata] {
  private val prefix = "doctorday"

  def deserialize(resolvedEvent: ResolvedEvent): Try[MessageEnvelope[EventMetadata]] = Try {
    val event = (resolvedEvent.getEvent.getEventType match {
      case s"$prefix-day-scheduled"          => decode[DayScheduled] _
      case s"$prefix-day-schedule-archived"  => decode[DayScheduleArchived] _
      case s"$prefix-slot-scheduled"         => decode[SlotScheduled] _
      case s"$prefix-slot-booked"            => decode[SlotBooked] _
      case s"$prefix-slot-booking-cancelled" => decode[SlotBookingCancelled] _
    })(new String(resolvedEvent.getEvent.getEventData)).toOption.get

    val metadata = decode[EventMetadata](new String(resolvedEvent.getEvent.getUserMetadata)).toOption.get

    toEnvelope(event, metadata, resolvedEvent)
  }

  def serialize(event: Any, metadata: EventMetadata): Try[ProposedEvent] = Try {
    event match {
      case e: DayScheduled         => toProposedEvent(s"$prefix-day-scheduled", e.asJson, metadata.asJson)
      case e: DayScheduleArchived  => toProposedEvent(s"$prefix-day-schedule-archivedd", e.asJson, metadata.asJson)
      case e: SlotScheduled        => toProposedEvent(s"$prefix-slot-scheduled", e.asJson, metadata.asJson)
      case e: SlotBooked           => toProposedEvent(s"$prefix-slot-booked", e.asJson, metadata.asJson)
      case e: SlotBookingCancelled => toProposedEvent(s"$prefix-slot-booking-cancelled", e.asJson, metadata.asJson)
      case c: CancelSlotBooking =>
        toProposedEvent(s"$prefix-command-cancel-slot-booking", c.asJson, metadata.asJson)
    }
  }
}
