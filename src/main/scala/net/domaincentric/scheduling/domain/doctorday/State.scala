package net.domaincentric.scheduling.domain.doctorday

import java.time.{ LocalDate, LocalTime }
import java.util.UUID

import net.domaincentric.scheduling.domain.doctorday.Planned.{ Slot, Slots }
import net.domaincentric.scheduling.eventsourcing.{ State => ESState }

import scala.concurrent.duration.Duration

sealed trait State extends ESState[State, Event]

case object Unscheduled extends State {
  def apply(event: Event): State = event match {
    case DayScheduled(id, _, date) => Planned(id, date, Slots.empty)
  }
}

case class Planned(id: UUID, date: LocalDate, slots: Slots) extends State {

  def apply(event: Event): State = event match {
    case slot: SlotScheduled                => copy(slots = slots.add(slot))
    case SlotBooked(_, slotId, _)           => copy(slots = slots.markAsBooked(slotId))
    case SlotBookingCancelled(_, slotId, _) => copy(slots = slots.markAsAvailable(slotId))
  }

  def hasAvailableSlot(slotId: UUID): Boolean = slots.value.filterNot(_.booked).exists(_.eventId == slotId)
  def hasBookedSlot(slotId: UUID): Boolean    = slots.value.filter(_.booked).exists(_.eventId == slotId)
  def doesNotOverlap(startTime: LocalTime, duration: Duration): Boolean =
    !slots.value.exists(_.overlapsWith(startTime, duration))
}

object Planned {
  case class Slot(eventId: UUID, startTime: LocalTime, duration: Duration, booked: Boolean = false) {
    def overlapsWith(otherStartTime: LocalTime, otherDuration: Duration): Boolean = {
      val firstStart  = startTime.toSecondOfDay
      val firstEnd    = startTime.plusSeconds(duration.toSeconds).toSecondOfDay
      val secondStart = otherStartTime.toSecondOfDay
      val secondEnd   = otherStartTime.plusSeconds(otherDuration.toSeconds).toSecondOfDay

      firstStart < secondEnd && secondStart < firstEnd
    }
  }
  case class Slots(value: Seq[Slot]) {
    def markAsAvailable(slotId: UUID): Slots =
      Slots(value.map { slot =>
        if (slot.eventId == slotId) slot.copy(booked = false)
        else slot
      })

    def markAsBooked(slotId: UUID): Slots =
      Slots(value.map { slot =>
        if (slot.eventId == slotId) slot.copy(booked = true)
        else slot
      })

    def add(slot: SlotScheduled): Slots =
      Slots(value.appended(Slot(slot.eventId, slot.startTime.toLocalTime, slot.duration)))
  }
  object Slots {
    def empty: Slots = Slots(Seq.empty)
  }
}
