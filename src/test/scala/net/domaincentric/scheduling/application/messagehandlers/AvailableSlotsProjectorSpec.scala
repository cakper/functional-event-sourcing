package net.domaincentric.scheduling.application.messagehandlers

import java.time.{ Duration => _, _ }

import net.domaincentric.scheduling.application.eventsourcing.{ MessageHandler, EventMetadata }
import net.domaincentric.scheduling.domain.writemodel.doctorday._
import net.domaincentric.scheduling.domain.readmodel.avialbleslots
import net.domaincentric.scheduling.domain.readmodel.avialbleslots.{ AvailableSlot, AvailableSlotsRepository }
import net.domaincentric.scheduling.test.{ EventHandlerSpec, MongoDb }
import net.domaincentric.scheduling.infrastructure.mongodb.MongodbAvailableSlotsRepository

import scala.concurrent.duration._

class AvailableSlotsProjectorSpec extends EventHandlerSpec with MongoDb {
  val today: LocalDate           = LocalDate.now(clock)
  val tenAm: LocalTime           = LocalTime.of(10, 0)
  val tenAmToday: LocalDateTime  = LocalDateTime.of(today, tenAm)
  val tenMinutes: FiniteDuration = 10.minutes

  val repository: AvailableSlotsRepository   = new MongodbAvailableSlotsRepository(database)
  val handler: MessageHandler[EventMetadata] = new AvailableSlotsProjector(repository)

  "available slots projector" should {
    "add slot to the list" in {
      val scheduled = SlotScheduled(SlotId.create, DayId(DoctorId("123"), today), tenAmToday, tenMinutes)
      `given`(scheduled)
      `then`(
        repository.getAvailableSlotsOn(today).map { result =>
          result shouldEqual Seq(
            AvailableSlot(
              scheduled.dayId,
              scheduled.slotId,
              scheduled.startTime.toLocalDate,
              scheduled.startTime.toLocalTime,
              scheduled.duration.toString()
            )
          )
        }
      )
    }

    "hide slot from the list if it was booked" in {
      val scheduled = SlotScheduled(SlotId.create, DayId(DoctorId("123"), today), tenAmToday, tenMinutes)
      val booked    = SlotBooked(scheduled.slotId, PatientId("John Doe"))
      `given`(scheduled, booked)
      `then`(repository.getAvailableSlotsOn(today).map(_ shouldEqual Seq.empty))
    }

    "show slot if booking was cancelled" in {
      val scheduled = SlotScheduled(SlotId.create, DayId(DoctorId("123"), today), tenAmToday, tenMinutes)
      val booked    = SlotBooked(scheduled.slotId, PatientId("John Doe"))
      val cancelled = SlotBookingCancelled(scheduled.slotId, "Can't make it")
      `given`(scheduled, booked, cancelled)
      `then`(
        repository.getAvailableSlotsOn(today).map { result =>
          result shouldEqual Seq(
            avialbleslots.AvailableSlot(
              scheduled.dayId,
              scheduled.slotId,
              scheduled.startTime.toLocalDate,
              scheduled.startTime.toLocalTime,
              scheduled.duration.toString()
            )
          )
        }
      )
    }

    "remove all slots from the list if the day was archived" in {
      val scheduled = SlotScheduled(SlotId.create, DayId(DoctorId("123"), today), tenAmToday, tenMinutes)
      val booked    = SlotBooked(scheduled.slotId, PatientId("John Doe"))
      val cancelled = SlotBookingCancelled(scheduled.slotId, "Can't make it")
      val archived  = DayScheduleArchived(scheduled.dayId)
      `given`(scheduled, booked, cancelled, archived)
      `then`(
        repository.getAvailableSlotsOn(today).map { result =>
          result shouldEqual Seq.empty
        }
      )
    }
  }
}
