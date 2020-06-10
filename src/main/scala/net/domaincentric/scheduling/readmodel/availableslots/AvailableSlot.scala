package net.domaincentric.scheduling.readmodel.availableslots

import java.time.LocalDateTime
import java.util.UUID

import scala.concurrent.duration.Duration

case class AvailableSlot(dayPlannedEventId: UUID, eventId: UUID, startTime: LocalDateTime, duration: Duration)
