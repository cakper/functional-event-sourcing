package net.domaincentric.scheduling.domain.aggregate.slot

import java.time.LocalDateTime
import java.util.UUID

import scala.concurrent.duration.Duration

sealed trait Event

case class Scheduled(slotId: UUID, startTime: LocalDateTime, duration: Duration) extends Event
case class Booked(slotId: UUID, patientId: String)                               extends Event
case class Cancelled(slotId: UUID, reason: String)                               extends Event
