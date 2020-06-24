package net.domaincentric.scheduling.domain.aggregate.doctorday

import java.time.{ LocalDate, LocalDateTime }
import java.util.UUID

import net.domaincentric.scheduling.domain.service.UuidGenerator

import scala.concurrent.duration.FiniteDuration

sealed trait Event

case class DayScheduled(dayId: DayId, doctorId: DoctorId, date: LocalDate)                                 extends Event
case class SlotScheduled(slotId: SlotId, dayId: DayId, startTime: LocalDateTime, duration: FiniteDuration) extends Event
case class SlotBooked(slotId: SlotId, patientId: PatientId)                                                extends Event
case class SlotBookingCancelled(slotId: SlotId, reason: String)                                            extends Event
case class DayScheduleCancelled(dayId: DayId, reason: String)                                              extends Event

case class DayId(value: String) {
  override def toString: String = value
}

object DayId {
  def apply(doctorId: DoctorId, date: LocalDate): DayId = DayId(s"${doctorId}_$date")
}

case class SlotId(value: UUID) {
  override def toString: String = value.toString
}

object SlotId {
  def create(implicit uuidGenerator: UuidGenerator): SlotId = SlotId(uuidGenerator.next())
}
