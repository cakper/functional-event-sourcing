package net.domaincentric.scheduling.domain.doctorday

import java.time.{ LocalDate, LocalDateTime }
import java.util.UUID

import net.domaincentric.scheduling.eventsourcing.{ Event => ESEvent }

import scala.concurrent.duration.Duration

sealed trait Event extends ESEvent

case class DayScheduled(eventId: UUID, doctorId: String, date: LocalDate) extends Event
case class SlotScheduled(eventId: UUID, dayPlannedEventId: UUID, startTime: LocalDateTime, duration: Duration)
    extends Event

case class SlotBooked(eventId: UUID, slotScheduledEventId: UUID, patientId: String)        extends Event
case class SlotBookingCancelled(eventId: UUID, slotScheduledEventId: UUID, reason: String) extends Event
