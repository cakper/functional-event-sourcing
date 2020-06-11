package net.domaincentric.scheduling.domain.readmodel.avialbleslots

import java.time.{ LocalDate, LocalTime }
import java.util.UUID

case class AvailableSlot(dayId: UUID, slotId: UUID, date: LocalDate, time: LocalTime, duration: String)
