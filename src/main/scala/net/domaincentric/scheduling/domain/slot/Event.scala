package net.domaincentric.scheduling.domain.slot

import java.time.LocalDateTime
import java.util.UUID

import net.domaincentric.scheduling.eventsourcing.{ Event => ESEvent }

import scala.concurrent.duration.Duration

sealed trait Event extends ESEvent

case class Scheduled(slotId: UUID, startTime: LocalDateTime, duration: Duration) extends Event
case class Booked(slotId: UUID, patientId: String)                               extends Event
case class Cancelled(slotId: UUID, reason: String)                               extends Event
