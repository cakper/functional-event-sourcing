package net.domaincentric.scheduling.readmodel.availableslots

import java.time.{ LocalDate, LocalTime }
import java.util.UUID

import scala.concurrent.duration.Duration

case class AvailableSlot(dayId: UUID, slotId: UUID, date: LocalDate, time: LocalTime, duration: String)
