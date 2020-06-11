package net.domaincentric.scheduling.domain.aggregate.slot

import java.time.LocalDateTime

import net.domaincentric.scheduling.domain.aggregate.AggregateSpec

import scala.concurrent.duration._

class SlotAggregateSpec extends AggregateSpec[Command, Event, Error, State] {

  override def state()   = UnscheduledSlot
  override def handler() = new CommandHandler

  "slot aggregate" should {
    "can be scheduled" in {
      `when`(Schedule(LocalDateTime.now(clock), 10.minutes))
      `then`(
        Scheduled(
          nextUuid(),
          Schedule(LocalDateTime.now(clock), 10.minutes).startTime,
          Schedule(LocalDateTime.now(clock), 10.minutes).duration
        )
      )
    }

    "can't be scheduled twice" in {
      `given`(Scheduled(nextUuid(), LocalDateTime.now(clock), 10.minutes))
      `when`(Schedule(LocalDateTime.now(clock), 10.minutes))
      `then`(SlotAlreadyScheduled())
    }

    "can be booked" in {
      val scheduled = Scheduled(nextUuid(), LocalDateTime.now(clock), 10.minutes)
      val book      = Book(randomString())
      `given`(scheduled)
      `when`(book)
      `then`(Booked(scheduled.slotId, book.patientId))
    }

    "can't be booked if it's not scheduled" in {
      `when`(Book(randomString()))
      `then`(SlotNotScheduled())
    }

    "can't be double booked" in {
      val scheduled = Scheduled(nextUuid(), LocalDateTime.now(clock), 10.minutes)
      val booked    = Booked(scheduled.slotId, randomString())
      val book      = Book(randomString())
      `given`(scheduled, booked)
      `when`(book)
      `then`(SlotAlreadyBooked())
    }

    "can be cancelled" in {
      val scheduled = Scheduled(nextUuid(), LocalDateTime.now(clock), 10.minutes)
      val booked    = Booked(scheduled.slotId, randomString())
      val cancel    = Cancel("No longer needed")
      `given`(scheduled, booked)
      `when`(cancel)
      `then`(Cancelled(scheduled.slotId, "No longer needed"))
    }

    "cancelled slot can be booked again" in {
      val scheduled = Scheduled(nextUuid(), LocalDateTime.now(clock), 10.minutes)
      val booked    = Booked(scheduled.slotId, randomString())
      val cancelled = Cancelled(scheduled.slotId, "No longer needed")
      val book      = Book(randomString())
      `given`(scheduled, booked, cancelled)
      `when`(book)
      `then`(Booked(scheduled.slotId, book.patientId))
    }

    "can't be cancelled if wasn't booked" in {
      val scheduled = Scheduled(nextUuid(), LocalDateTime.now(clock), 10.minutes)
      `given`(scheduled)
      `when`(Cancel("No longer needed"))
      `then`(SlotNotBooked())
    }

    "can't be cancelled after start time" in {
      val scheduled = Scheduled(nextUuid(), LocalDateTime.now(clock).minusHours(1), 10.minutes)
      val booked    = Booked(scheduled.slotId, randomString())
      val cancel    = Cancel("No longer needed")
      `given`(scheduled, booked)
      `when`(cancel)
      `then`(SlotAlreadyStarted())
    }
  }
}
