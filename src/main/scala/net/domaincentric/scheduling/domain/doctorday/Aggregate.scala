package net.domaincentric.scheduling.domain.doctorday

import java.time.{ Clock, LocalDateTime }

import net.domaincentric.scheduling.domain.doctorday.Aggregate.handler
import net.domaincentric.scheduling.eventsourcing.Aggregate._
import net.domaincentric.scheduling.eventsourcing.{ UuidGenerator, Aggregate => ESAggregate }

class Aggregate(id: String)(implicit uuidGenerator: UuidGenerator, clock: Clock)
    extends ESAggregate[Command, Event, Error, State](id, Unscheduled, handler)
object Aggregate {
  def handler(implicit idGen: UuidGenerator, clock: Clock): Handler[Command, Event, Error, State] = {
    case (Unscheduled, scheduleDay: ScheduleDay) =>
      val dayPlannedEventId = idGen.next()
      DayScheduled(dayPlannedEventId, scheduleDay.doctorId, scheduleDay.date) :: scheduleDay.slots.map { slot =>
        SlotScheduled(
          dayPlannedEventId,
          idGen.next(),
          LocalDateTime.of(scheduleDay.date, slot.startTime),
          slot.duration
        )
      }.toList

    case (_: Planned, _: ScheduleDay) => DayAlreadyScheduled

    case (state: Planned, ScheduleSlot(startTime, duration)) if state.doesNotOverlap(startTime, duration) =>
      SlotScheduled(state.id, idGen.next(), LocalDateTime.of(state.date, startTime), duration)

    case (_: Planned, _: ScheduleSlot) => SlotOverlapped

    case (state: Planned, BookSlot(slotId, patientId)) if state.hasAvailableSlot(slotId) =>
      SlotBooked(slotId, patientId)

    case (state: Planned, BookSlot(slotId, _)) if state.hasBookedSlot(slotId) => SlotAlreadyBooked

    case (_: Planned, _: BookSlot) => SlotNotScheduled

    case (state: Planned, CancelSlotBooking(slotId, reason)) if state.hasBookedSlot(slotId) =>
      SlotBookingCancelled(slotId, reason)

    case (_: Planned, _: CancelSlotBooking) => SlotNotBooked
  }
}
