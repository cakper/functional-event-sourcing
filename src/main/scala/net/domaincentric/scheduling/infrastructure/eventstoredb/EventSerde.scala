package net.domaincentric.scheduling.infrastructure.eventstoredb

import java.util.UUID
import java.util.concurrent.TimeUnit

import com.eventstore.dbclient.{ ProposedEvent, ResolvedEvent }
import io.circe._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.{ EventEnvelope, EventMetadata }
import net.domaincentric.scheduling.domain.aggregate.doctorday.{ DayScheduled, SlotBooked, SlotBookingCancelled, SlotScheduled }

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class EventSerde {
  val prefix = s"doctorday"

  implicit final val finiteDurationDecoder: Decoder[FiniteDuration] =
    (c: HCursor) =>
      for {
        length     <- c.downField("length").as[Long]
        unitString <- c.downField("unit").as[String]
        unit <- try {
          Right(TimeUnit.valueOf(unitString))
        } catch {
          case _: IllegalArgumentException => Left(DecodingFailure("FiniteDuration", c.history))
        }
      } yield FiniteDuration(length, unit)

  implicit final val finiteDurationEncoder: Encoder[FiniteDuration] = (a: FiniteDuration) =>
    Json.fromJsonObject(JsonObject("length" -> Json.fromLong(a.length), "unit" -> Json.fromString(a.unit.name)))

  def deserialize(resolvedEvent: ResolvedEvent): Try[EventEnvelope] = Try {
    val rawEvent = resolvedEvent.getEvent
    val event = (rawEvent.getEventType match {
      case s"$prefix-day-scheduled"          => decode[DayScheduled] _
      case s"$prefix-slot-scheduled"         => decode[SlotScheduled] _
      case s"$prefix-slot-booked"            => decode[SlotBooked] _
      case s"$prefix-slot-booking-cancelled" => decode[SlotBookingCancelled] _
    })(new String(rawEvent.getEventData)).toOption.get

    val metadata = decode[EventMetadata](new String(rawEvent.getUserMetadata)).toOption.get
    eventsourcing.EventEnvelope(
      event,
      metadata,
      rawEvent.getEventId,
      rawEvent.getStreamRevision.getValueUnsigned,
      rawEvent.getCreated
    )
  }

  def serialize(event: Any, metadata: EventMetadata): Try[ProposedEvent] = Try {
    event match {
      case e: DayScheduled         => toProposedEvent(s"$prefix-day-scheduled", e.asJson, metadata)
      case e: SlotScheduled        => toProposedEvent(s"$prefix-slot-scheduled", e.asJson, metadata)
      case e: SlotBooked           => toProposedEvent(s"$prefix-slot-booked", e.asJson, metadata)
      case e: SlotBookingCancelled => toProposedEvent(s"$prefix-slot-booking-cancelled", e.asJson, metadata)
    }
  }

  private def toProposedEvent(eventType: String, data: Json, metadata: EventMetadata) = {
    new ProposedEvent(
      UUID.randomUUID(),
      eventType,
      "application/json",
      data.noSpaces.getBytes,
      metadata.asJson.noSpaces.getBytes
    )
  }
}
