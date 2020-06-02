package net.domaincentric.scheduling.domain.slot

import java.time.Clock

import net.domaincentric.scheduling.domain.slot.SlotAggregate.handler
import net.domaincentric.scheduling.eventsourcing.Aggregate._
import net.domaincentric.scheduling.eventsourcing.{ Aggregate, UuidGenerator }

class SlotAggregate(id: String)(implicit uuidGenerator: UuidGenerator, clock: Clock)
    extends Aggregate[Command, Event, Error, SlotState](id, UnscheduledSlot, handler)
object SlotAggregate {
  def handler(implicit uuidGenerator: UuidGenerator, clock: Clock): Handler[Command, Event, Error, SlotState] = {
    case (_: ScheduledSlot, _: Schedule)                            => SlotAlreadyScheduled()
    case (UnscheduledSlot, Schedule(startTime, duration))           => Scheduled(uuidGenerator.nextUuid(), startTime, duration)
    case (UnscheduledSlot, _)                                       => SlotNotScheduled()
    case (_: BookedSlot, _: Book)                                   => SlotAlreadyBooked()
    case (ScheduledSlot(eventId, _, _), Book(patientId))            => Booked(uuidGenerator.nextUuid(), eventId, patientId)
    case (_: ScheduledSlot, _: Cancel)                              => SlotNotBooked()
    case (booked: BookedSlot, _: Cancel) if booked.isStarted(clock) => SlotAlreadyStarted()
    case (BookedSlot(eventId, _, _, _), Cancel(reason))             => Cancelled(uuidGenerator.nextUuid(), eventId, reason)
  }
}
