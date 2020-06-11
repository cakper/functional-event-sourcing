package net.domaincentric.scheduling.domain.aggregate.slot

import java.time.{ Clock, LocalDateTime }
import java.util.UUID

import net.domaincentric.scheduling.domain.aggregate

import scala.concurrent.duration.Duration

sealed trait State extends aggregate.State[State, Event]

case object UnscheduledSlot extends State {
  def apply(event: Event): State = event match {
    case Scheduled(eventId, startTime, duration) =>
      ScheduledSlot(eventId, startTime, duration)
    case _ => this
  }
}

case class ScheduledSlot(scheduledEventId: UUID, startTime: LocalDateTime, duration: Duration) extends State {

  def apply(event: Event): State = event match {
    case Booked(_, patientId) => BookedSlot(scheduledEventId, startTime, duration, patientId)
    case _                    => this
  }
}

case class BookedSlot(scheduledEventId: UUID, startTime: LocalDateTime, duration: Duration, patientId: String)
    extends State {
  def apply(event: Event): State = event match {
    case Cancelled(_, _) => ScheduledSlot(scheduledEventId, startTime, duration)
    case _               => this
  }

  def isStarted(clock: Clock): Boolean = LocalDateTime.now(clock).isAfter(startTime)
}
