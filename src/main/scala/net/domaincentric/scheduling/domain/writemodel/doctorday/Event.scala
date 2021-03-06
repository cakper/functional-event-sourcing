package net.domaincentric.scheduling.domain.writemodel.doctorday

import java.time.{ LocalDate, LocalDateTime }
import java.util.UUID

import net.domaincentric.scheduling.domain.service.UuidGenerator

import scala.concurrent.duration.FiniteDuration

sealed trait Event

case class DayScheduled(dayId: DayId, doctorId: DoctorId, date: LocalDate) extends Event

case class SlotScheduled(slotId: SlotId, dayId: DayId, startTime: LocalDateTime, duration: FiniteDuration) extends Event
case class SlotCancelled(slotId: SlotId)                                                                   extends Event
case class SlotBooked(slotId: SlotId, patientId: PatientId)                                                extends Event
case class SlotBookingCancelled(slotId: SlotId, reason: String)                                            extends Event
case class DayScheduleCancelled(dayId: DayId, reason: String)                                              extends Event
case class DayScheduleArchived(dayId: DayId)                                                               extends Event
