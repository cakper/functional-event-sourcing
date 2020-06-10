package net.domaincentric.scheduling.domain.doctorday

import java.time.{ LocalDate, LocalDateTime, LocalTime }

import net.domaincentric.scheduling.domain.doctorday.ScheduleDay.Slot
import net.domaincentric.scheduling.eventsourcing.Aggregate.Handler
import net.domaincentric.scheduling.eventsourcing.AggregateSpec

import scala.concurrent.duration._

class DoctorDayAggregateSpec extends AggregateSpec[Command, Event, Error, State] {
  override def state(): State = Unscheduled

  override def handler(): Handler[Command, Event, Error, State] = Aggregate.handler

  private val today: LocalDate           = LocalDate.now(clock)
  private val tenAm: LocalTime           = LocalTime.of(10, 0)
  private val tenAmToday: LocalDateTime  = LocalDateTime.of(today, tenAm)
  private val tenMinutes: FiniteDuration = 10.minutes

  "doctor day aggregate" should {
    "be scheduled" in {
      val startTime = LocalTime.of(9, 0)
      val slots     = 0.until(300).by(10).map(i => Slot(startTime.plusMinutes(i), tenMinutes)).toList
      val schedule  = ScheduleDay(randomString(), today, slots)
      `when`(schedule)

      val dayScheduled = DayScheduled(nextUuid(), schedule.doctorId, schedule.date)
      val slotsScheduled = slots.map { s =>
        SlotScheduled(nextUuid(), dayScheduled.eventId, LocalDateTime.of(today, s.startTime), s.duration)
      }

      `then`(dayScheduled :: slotsScheduled: _*)
    }

    "not be scheduled twice" in {
      val dayScheduled = DayScheduled(nextUuid(), randomString(), today)

      val startTime = LocalTime.of(9, 0)
      val slots     = 0.until(300).by(10).map(i => Slot(startTime.plusMinutes(i), tenMinutes)).toList
      val schedule  = ScheduleDay(randomString(), today, slots)

      `given`(dayScheduled)
      `when`(schedule)
      `then`(DayAlreadyScheduled)
    }

    "allow to book a slot" in {
      val dayScheduled  = DayScheduled(nextUuid(), randomString(), today)
      val slotScheduled = SlotScheduled(nextUuid(), dayScheduled.eventId, tenAmToday, 10.minute)
      val book          = BookSlot(slotScheduled.eventId, randomString())

      `given`(dayScheduled, slotScheduled)
      `when`(book)
      `then`(SlotBooked(nextUuid(), slotScheduled.eventId, book.patientId))
    }

    "not allow to book a slot twice" in {
      val dayScheduled  = DayScheduled(nextUuid(), randomString(), today)
      val slotScheduled = SlotScheduled(nextUuid(), dayScheduled.eventId, tenAmToday, 10.minute)
      val slotBooked    = SlotBooked(nextUuid(), slotScheduled.eventId, randomString())
      val book          = BookSlot(slotScheduled.eventId, randomString())

      `given`(dayScheduled, slotScheduled, slotBooked)
      `when`(book)
      `then`(SlotAlreadyBooked)
    }

    "not allow to book unscheduled slot" in {
      val dayScheduled  = DayScheduled(nextUuid(), randomString(), today)
      val slotScheduled = SlotScheduled(nextUuid(), dayScheduled.eventId, tenAmToday, 10.minute)
      val book          = BookSlot(nextUuid(), randomString())

      `given`(dayScheduled, slotScheduled)
      `when`(book)
      `then`(SlotNotScheduled)
    }

    "allow to cancel booking" in {
      val dayScheduled  = DayScheduled(nextUuid(), randomString(), today)
      val slotScheduled = SlotScheduled(nextUuid(), dayScheduled.eventId, tenAmToday, 10.minute)
      val slotBooked    = SlotBooked(nextUuid(), slotScheduled.eventId, randomString())

      val cancel = CancelSlotBooking(slotScheduled.eventId, randomString())

      `given`(dayScheduled, slotScheduled, slotBooked)
      `when`(cancel)
      `then`(SlotBookingCancelled(nextUuid(), slotScheduled.eventId, cancel.reason))
    }

    "not allow to cancel an unbooked slot" in {
      val dayScheduled  = DayScheduled(nextUuid(), randomString(), today)
      val slotScheduled = SlotScheduled(nextUuid(), dayScheduled.eventId, tenAmToday, 10.minute)

      val cancel = CancelSlotBooking(slotScheduled.eventId, randomString())

      `given`(dayScheduled, slotScheduled)
      `when`(cancel)
      `then`(SlotNotBooked)
    }

    "allow to schedule an extra slot" in {
      val dayScheduled = DayScheduled(nextUuid(), randomString(), today)
      `given`(dayScheduled)
      `when`(ScheduleSlot(tenAm, tenMinutes))
      `then`(SlotScheduled(nextUuid(), dayScheduled.eventId, tenAmToday, tenMinutes))
    }

    "forbid to schedule overlapping slots" in {
      val dayScheduled  = DayScheduled(nextUuid(), randomString(), today)
      val slotScheduled = SlotScheduled(nextUuid(), dayScheduled.eventId, tenAmToday, tenMinutes)
      `given`(dayScheduled, slotScheduled)
      `when`(ScheduleSlot(tenAm, tenMinutes))
      `then`(SlotOverlapped)
    }

    "allow to schedule adjacent slots" in {
      val dayScheduled  = DayScheduled(nextUuid(), randomString(), today)
      val slotScheduled = SlotScheduled(nextUuid(), dayScheduled.eventId, tenAmToday, tenMinutes)
      `given`(dayScheduled, slotScheduled)
      `when`(ScheduleSlot(tenAm.plusMinutes(10), tenMinutes))
      `then`(SlotScheduled(nextUuid(), dayScheduled.eventId, tenAmToday.plusMinutes(10), tenMinutes))
    }
  }
}
