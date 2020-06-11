package net.domaincentric.scheduling.domain.aggregate.slot

import java.time.Clock

import net.domaincentric.scheduling.domain.aggregate
import net.domaincentric.scheduling.domain.aggregate.CommandHandler._
import net.domaincentric.scheduling.domain.service.UuidGenerator

class CommandHandler(implicit uuidGenerator: UuidGenerator, clock: Clock)
    extends aggregate.CommandHandler[Command, Event, Error, State] {
  override def apply(state: State, command: Command): Either[Error, Seq[Event]] = (state, command) match {
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
