package net.domaincentric.scheduling.domain.aggregate.doctorday

import java.time.LocalDateTime
import java.util.UUID

import net.domaincentric.scheduling.domain.aggregate
import net.domaincentric.scheduling.domain.aggregate.CommandHandler._
import net.domaincentric.scheduling.domain.service.UuidGenerator

class CommandHandler(implicit idGen: UuidGenerator) extends aggregate.CommandHandler[Command, Event, Error, State] {
  override def apply(state: State, command: Command): Either[Error, Seq[Event]] = (state, command) match {
    case (Unscheduled, scheduleDay: ScheduleDay) =>
      val dayId: DayId = DayId.create
      DayScheduled(dayId, scheduleDay.doctorId, scheduleDay.date) :: scheduleDay.slots.map { slot =>
        SlotScheduled(
          SlotId.create,
          dayId,
          LocalDateTime.of(scheduleDay.date, slot.startTime),
          slot.duration
        )
      }.toList

    case (_: Scheduled, _: ScheduleDay) => DayAlreadyScheduled

    case (state: Scheduled, CancelDaySchedule(reason)) =>
      DayScheduleCancelled(state.dayId, reason) :: state.allBookedSlots
        .map(slot => SlotBookingCancelled(slot.slotId, reason))
        .toList

    case (state: Scheduled, ScheduleSlot(startTime, duration)) if state.doesNotOverlap(startTime, duration) =>
      SlotScheduled(SlotId.create, state.dayId, LocalDateTime.of(state.date, startTime), duration)

    case (_: Scheduled, _: ScheduleSlot) => SlotOverlapped

    case (state: Scheduled, BookSlot(slotId, patientId)) if state.hasAvailableSlot(slotId) =>
      SlotBooked(slotId, patientId)

    case (state: Scheduled, BookSlot(slotId, _)) if state.hasBookedSlot(slotId) => SlotAlreadyBooked

    case (_: Scheduled, _: BookSlot) => SlotNotScheduled

    case (state: Scheduled, CancelSlotBooking(slotId, reason)) if state.hasBookedSlot(slotId) =>
      SlotBookingCancelled(slotId, reason)

    case (_: Scheduled, _: CancelSlotBooking) => SlotNotBooked
  }
}
