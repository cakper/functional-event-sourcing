package net.domaincentric.scheduling.domain.doctorday

import java.time.{ LocalDate, LocalTime }
import java.util.UUID

import net.domaincentric.scheduling.eventsourcing.{ Command => ESCommand }

import scala.concurrent.duration.Duration

sealed trait Command extends ESCommand

case class ScheduleDay(doctorId: String, date: LocalDate, slots: Seq[ScheduleDay.Slot]) extends Command

object ScheduleDay {
  case class Slot(startTime: LocalTime, duration: Duration)
}

case class ScheduleSlot(startTime: LocalTime, duration: Duration) extends Command
case class BookSlot(slotId: UUID, patientId: String)              extends Command
case class CancelSlotBooking(slotId: UUID, reason: String)        extends Command
