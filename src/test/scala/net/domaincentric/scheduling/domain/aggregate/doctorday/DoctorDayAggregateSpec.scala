package net.domaincentric.scheduling.domain.aggregate.doctorday

import java.time.{ LocalDate, LocalDateTime, LocalTime }

import net.domaincentric.scheduling.domain.aggregate.AggregateSpec
import net.domaincentric.scheduling.domain.aggregate.doctorday.ScheduleDay.Slot

import scala.concurrent.duration._

class DoctorDayAggregateSpec extends AggregateSpec[Command, Event, Error, State] {
  override def state() = Unscheduled

  override def handler() = new CommandHandler

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

      val dayScheduled = DayScheduled(DayId.create, schedule.doctorId, schedule.date)
      val slotsScheduled = slots.map { s =>
        SlotScheduled(SlotId.create, dayScheduled.dayId, LocalDateTime.of(today, s.startTime), s.duration)
      }

      `then`(dayScheduled :: slotsScheduled: _*)
    }

    "not be scheduled twice" in {
      val dayScheduled = DayScheduled(DayId.create, randomString(), today)

      val startTime = LocalTime.of(9, 0)
      val slots     = 0.until(300).by(10).map(i => Slot(startTime.plusMinutes(i), tenMinutes)).toList
      val schedule  = ScheduleDay(randomString(), today, slots)

      `given`(dayScheduled)
      `when`(schedule)
      `then`(DayAlreadyScheduled)
    }

    "allow to book a slot" in {
      val dayScheduled  = DayScheduled(DayId.create, randomString(), today)
      val slotScheduled = SlotScheduled(SlotId.create, dayScheduled.dayId, tenAmToday, 10.minute)
      val book          = BookSlot(slotScheduled.slotId, randomString())

      `given`(dayScheduled, slotScheduled)
      `when`(book)
      `then`(SlotBooked(slotScheduled.slotId, book.patientId))
    }

    "not allow to book a slot twice" in {
      val dayScheduled  = DayScheduled(DayId.create, randomString(), today)
      val slotScheduled = SlotScheduled(SlotId.create, dayScheduled.dayId, tenAmToday, 10.minute)
      val slotBooked    = SlotBooked(slotScheduled.slotId, randomString())
      val book          = BookSlot(slotScheduled.slotId, randomString())

      `given`(dayScheduled, slotScheduled, slotBooked)
      `when`(book)
      `then`(SlotAlreadyBooked)
    }

    "not allow to book unscheduled slot" in {
      val dayScheduled  = DayScheduled(DayId.create, randomString(), today)
      val slotScheduled = SlotScheduled(SlotId.create, dayScheduled.dayId, tenAmToday, 10.minute)
      val book          = BookSlot(SlotId.create, randomString())

      `given`(dayScheduled, slotScheduled)
      `when`(book)
      `then`(SlotNotScheduled)
    }

    "allow to cancel booking" in {
      val dayScheduled  = DayScheduled(DayId.create, randomString(), today)
      val slotScheduled = SlotScheduled(SlotId.create, dayScheduled.dayId, tenAmToday, 10.minute)
      val slotBooked    = SlotBooked(slotScheduled.slotId, randomString())

      val cancel = CancelSlotBooking(slotScheduled.slotId, randomString())

      `given`(dayScheduled, slotScheduled, slotBooked)
      `when`(cancel)
      `then`(SlotBookingCancelled(slotScheduled.slotId, cancel.reason))
    }

    "not allow to cancel an unbooked slot" in {
      val dayScheduled  = DayScheduled(DayId.create, randomString(), today)
      val slotScheduled = SlotScheduled(SlotId.create, dayScheduled.dayId, tenAmToday, 10.minute)

      val cancel = CancelSlotBooking(slotScheduled.slotId, randomString())

      `given`(dayScheduled, slotScheduled)
      `when`(cancel)
      `then`(SlotNotBooked)
    }

    "allow to schedule an extra slot" in {
      val dayScheduled = DayScheduled(DayId.create, randomString(), today)
      `given`(dayScheduled)
      `when`(ScheduleSlot(tenAm, tenMinutes))
      `then`(SlotScheduled(SlotId.create, dayScheduled.dayId, tenAmToday, tenMinutes))
    }

    "forbid to schedule overlapping slots" in {
      val dayScheduled  = DayScheduled(DayId.create, randomString(), today)
      val slotScheduled = SlotScheduled(SlotId.create, dayScheduled.dayId, tenAmToday, tenMinutes)
      `given`(dayScheduled, slotScheduled)
      `when`(ScheduleSlot(tenAm, tenMinutes))
      `then`(SlotOverlapped)
    }

    "allow to schedule adjacent slots" in {
      val dayScheduled  = DayScheduled(DayId.create, randomString(), today)
      val slotScheduled = SlotScheduled(SlotId.create, dayScheduled.dayId, tenAmToday, tenMinutes)
      `given`(dayScheduled, slotScheduled)
      `when`(ScheduleSlot(tenAm.plusMinutes(10), tenMinutes))
      `then`(SlotScheduled(SlotId.create, dayScheduled.dayId, tenAmToday.plusMinutes(10), tenMinutes))
    }

    "cancel booked slots when the day is cancelled" in {
      val dayScheduled   = DayScheduled(DayId.create, randomString(), today)
      val slotScheduled1 = SlotScheduled(SlotId.create, dayScheduled.dayId, tenAmToday, tenMinutes)
      val slotScheduled2 = SlotScheduled(SlotId.create, dayScheduled.dayId, tenAmToday.plusMinutes(10), tenMinutes)
      val slotBooked     = SlotBooked(slotScheduled1.slotId, randomString())
      `given`(dayScheduled, slotScheduled1, slotScheduled2, slotBooked)

      val reason = "doctor cancelled"
      `when`(CancelDaySchedule(reason))

      `then`(
        DayScheduleCancelled(dayScheduled.dayId, reason),
        SlotBookingCancelled(slotScheduled1.slotId, reason)
      )
    }
  }
}
