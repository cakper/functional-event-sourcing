package net.domaincentric.scheduling.domain.slot

import java.time.Clock

import net.domaincentric.scheduling.eventsourcing
import net.domaincentric.scheduling.eventsourcing.CommandHandler._
import net.domaincentric.scheduling.eventsourcing.UuidGenerator

class CommandHandler(implicit uuidGenerator: UuidGenerator, clock: Clock)
    extends eventsourcing.CommandHandler[Command, Event, Error, SlotState] {
  override def apply(state: SlotState, command: Command): Either[Error, Seq[Event]] = (state, command) match {
    case (_: ScheduledSlot, _: Schedule)                            => SlotAlreadyScheduled()
    case (UnscheduledSlot, Schedule(startTime, duration))           => Scheduled(uuidGenerator.next(), startTime, duration)
    case (UnscheduledSlot, _)                                       => SlotNotScheduled()
    case (_: BookedSlot, _: Book)                                   => SlotAlreadyBooked()
    case (ScheduledSlot(slotId, _, _), Book(patientId))             => Booked(slotId, patientId)
    case (_: ScheduledSlot, _: Cancel)                              => SlotNotBooked()
    case (booked: BookedSlot, _: Cancel) if booked.isStarted(clock) => SlotAlreadyStarted()
    case (BookedSlot(eventId, _, _, _), Cancel(reason))             => Cancelled(eventId, reason)
  }
}
