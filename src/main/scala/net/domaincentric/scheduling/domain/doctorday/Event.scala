package net.domaincentric.scheduling.domain.doctorday

import java.time.{ LocalDate, LocalDateTime }
import java.util.UUID

import net.domaincentric.scheduling.eventsourcing.{ Event => ESEvent }

import scala.concurrent.duration.FiniteDuration

sealed trait Event extends ESEvent

case class DayScheduled(dayId: UUID, doctorId: String, date: LocalDate)                                 extends Event
case class SlotScheduled(slotId: UUID, dayId: UUID, startTime: LocalDateTime, duration: FiniteDuration) extends Event
case class SlotBooked(slotId: UUID, patientId: String)                                                  extends Event
case class SlotBookingCancelled(slotId: UUID, reason: String)                                           extends Event
