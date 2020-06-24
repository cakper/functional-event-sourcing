package net.domaincentric.scheduling.domain.aggregate.doctorday

import java.time.{ LocalDate, LocalTime }

import net.domaincentric.scheduling.domain.aggregate
import net.domaincentric.scheduling.domain.aggregate.doctorday.Scheduled.{ Slot, Slots }

import scala.concurrent.duration.{ Duration, FiniteDuration }

sealed trait State extends aggregate.State[State, Event]

case object Unscheduled extends State {
  def apply(event: Event): State = event match {
    case DayScheduled(id, _, date) => Scheduled(id, date, Slots.empty)
    case DayScheduleArchived(_)    => Archived
  }
}

case class Scheduled(dayId: DayId, date: LocalDate, slots: Slots) extends State {

  def apply(event: Event): State = event match {
    case slot: SlotScheduled             => copy(slots = slots.add(slot))
    case SlotBooked(slotId, _)           => copy(slots = slots.markAsBooked(slotId))
    case SlotBookingCancelled(slotId, _) => copy(slots = slots.markAsAvailable(slotId))
    case DayScheduleCancelled(_, _)      => Cancelled
    case DayScheduleArchived(_)          => Archived
  }

  def hasAvailableSlot(slotId: SlotId): Boolean = slots.value.filterNot(_.booked).exists(_.slotId == slotId)
  def hasBookedSlot(slotId: SlotId): Boolean    = slots.value.filter(_.booked).exists(_.slotId == slotId)
  def doesNotOverlap(startTime: LocalTime, duration: Duration): Boolean =
    !slots.value.exists(_.overlapsWith(startTime, duration))
  def allBookedSlots: Seq[Slot] = slots.value.filter(_.booked)
}

object Scheduled {
  case class Slot(slotId: SlotId, startTime: LocalTime, duration: FiniteDuration, booked: Boolean = false) {
    def overlapsWith(otherStartTime: LocalTime, otherDuration: Duration): Boolean = {
      val firstStart  = startTime.toSecondOfDay
      val firstEnd    = startTime.plusSeconds(duration.toSeconds).toSecondOfDay
      val secondStart = otherStartTime.toSecondOfDay
      val secondEnd   = otherStartTime.plusSeconds(otherDuration.toSeconds).toSecondOfDay

      firstStart < secondEnd && secondStart < firstEnd
    }
  }
  case class Slots(value: Seq[Slot]) {
    def markAsAvailable(slotId: SlotId): Slots =
      Slots(value.map { slot =>
        if (slot.slotId == slotId) slot.copy(booked = false)
        else slot
      })

    def markAsBooked(slotId: SlotId): Slots =
      Slots(value.map { slot =>
        if (slot.slotId == slotId) slot.copy(booked = true)
        else slot
      })

    def add(slot: SlotScheduled): Slots =
      Slots(value.appended(Slot(slot.slotId, slot.startTime.toLocalTime, slot.duration)))
  }
  object Slots {
    def empty: Slots = Slots(Seq.empty)
  }
}

case object Archived extends State {
  def apply(event: Event): State = event match {
    case _ => this
  }
}

case object Cancelled extends State {
  def apply(event: Event): State = event match {
    case DayScheduleArchived(_) => Archived
    case _                      => this
  }
}
