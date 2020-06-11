package net.domaincentric.scheduling.domain.aggregate.doctorday

import java.time.{ LocalDate, LocalTime }
import java.util.UUID

import scala.concurrent.duration.FiniteDuration

sealed trait Command

case class ScheduleDay(doctorId: String, date: LocalDate, slots: Seq[ScheduleDay.Slot]) extends Command

object ScheduleDay {
  case class Slot(startTime: LocalTime, duration: FiniteDuration)
}

case class ScheduleSlot(startTime: LocalTime, duration: FiniteDuration) extends Command
case class BookSlot(slotId: UUID, patientId: String)                    extends Command
case class CancelSlotBooking(slotId: UUID, reason: String)              extends Command
