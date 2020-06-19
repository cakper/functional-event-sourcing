package net.domaincentric.scheduling.domain.readmodel.avialbleslots

import java.time.{ LocalDate, LocalTime }

import net.domaincentric.scheduling.domain.aggregate.doctorday.{ DayId, SlotId }

case class AvailableSlot(dayId: DayId, slotId: SlotId, date: LocalDate, time: LocalTime, duration: String)
