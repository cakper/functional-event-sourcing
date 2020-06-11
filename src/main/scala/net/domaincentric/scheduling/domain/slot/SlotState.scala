package net.domaincentric.scheduling.domain.slot

import java.time.{ Clock, LocalDateTime }
import java.util.UUID

import net.domaincentric.scheduling.eventsourcing.State

import scala.concurrent.duration.Duration

sealed trait SlotState extends State[SlotState, Event]

case object UnscheduledSlot extends SlotState {
  def apply(event: Event): SlotState = event match {
    case Scheduled(eventId, startTime, duration) =>
      ScheduledSlot(eventId, startTime, duration)
  }
}

case class ScheduledSlot(scheduledEventId: UUID, startTime: LocalDateTime, duration: Duration) extends SlotState {

  def apply(event: Event): SlotState = event match {
    case Booked(_, patientId) => BookedSlot(scheduledEventId, startTime, duration, patientId)
  }
}

case class BookedSlot(scheduledEventId: UUID, startTime: LocalDateTime, duration: Duration, patientId: String)
    extends SlotState {
  def apply(event: Event): SlotState = event match {
    case Cancelled(_, _) => ScheduledSlot(scheduledEventId, startTime, duration)
  }

  def isStarted(clock: Clock): Boolean = LocalDateTime.now(clock).isAfter(startTime)
}
