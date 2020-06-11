package net.domaincentric.scheduling.domain.doctorday

import java.time.LocalDateTime
import java.util.UUID

import net.domaincentric.scheduling.eventsourcing
import net.domaincentric.scheduling.eventsourcing.CommandHandler._
import net.domaincentric.scheduling.eventsourcing.UuidGenerator

class CommandHandler(implicit idGen: UuidGenerator) extends eventsourcing.CommandHandler[Command, Event, Error, State] {
  override def apply(state: State, command: Command): Either[Error, Seq[Event]] = (state, command) match {
    case (Unscheduled, scheduleDay: ScheduleDay) =>
      val dayPlannedEventId: UUID = idGen.next()
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
