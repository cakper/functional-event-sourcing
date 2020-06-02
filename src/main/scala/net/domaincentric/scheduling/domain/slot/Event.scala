package net.domaincentric.scheduling.domain.slot

import java.time.LocalDateTime
import java.util.UUID

import net.domaincentric.scheduling.eventsourcing.{Event => ESEvent}

import scala.concurrent.duration.Duration

sealed trait Event extends ESEvent

case class Scheduled(eventId: UUID, startTime: LocalDateTime, duration: Duration) extends Event
case class Booked(eventId: UUID, scheduledEventId: UUID, patientId: String)       extends Event
case class Cancelled(eventId: UUID, scheduledEventId: UUID, reason: String)       extends Event
