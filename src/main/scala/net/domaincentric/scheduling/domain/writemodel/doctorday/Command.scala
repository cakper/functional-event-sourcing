package net.domaincentric.scheduling.domain.writemodel.doctorday

import java.time.{ LocalDate, LocalTime }

import scala.concurrent.duration.FiniteDuration

sealed trait Command

case class ScheduleDay(doctorId: DoctorId, date: LocalDate, slots: Seq[ScheduleDay.Slot]) extends Command

object ScheduleDay {
  case class Slot(startTime: LocalTime, duration: FiniteDuration)
}

case class ScheduleSlot(startTime: LocalTime, duration: FiniteDuration) extends Command
case class BookSlot(slotId: SlotId, patientId: PatientId)               extends Command
case class CancelSlotBooking(slotId: SlotId, reason: String)            extends Command
case class CancelDaySchedule(reason: String)                            extends Command
case class Archive()                                                    extends Command
