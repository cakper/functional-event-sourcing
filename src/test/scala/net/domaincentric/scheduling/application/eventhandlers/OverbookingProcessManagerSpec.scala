package net.domaincentric.scheduling.application.eventhandlers

import java.time.{ LocalDate, LocalDateTime, LocalTime }

import net.domaincentric.scheduling.application.eventsourcing.CommandBus.CommandEnvelope
import net.domaincentric.scheduling.application.eventsourcing.{ AggregateId, CausationId, CommandMetadata, MessageHandler }
import net.domaincentric.scheduling.domain.aggregate.doctorday._
import net.domaincentric.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository
import net.domaincentric.scheduling.infrastructure.mongodb.MongoDbBookedSlotsRepository
import net.domaincentric.scheduling.test.{ EventHandlerSpec, MongoDbSpec }

import scala.concurrent.duration._

class OverbookingProcessManagerSpec extends EventHandlerSpec with MongoDbSpec {
  override def enableAtLeastOnceMonkey = false
  override def enableWonkyIoMonkey     = false

  val today: LocalDate           = LocalDate.now(clock)
  val tenAm: LocalTime           = LocalTime.of(10, 0)
  val tenAmToday: LocalDateTime  = LocalDateTime.of(today, tenAm)
  val tenMinutes: FiniteDuration = 10.minutes

  val repository: BookedSlotsRepository = new MongoDbBookedSlotsRepository(database)

  private val bookingLimitPerPatient = 3

  override def handler = new OverbookingProcessManager(repository, commandBus, bookingLimitPerPatient)

  "overbooking process manager" should {
    "increment the visit counter every time a patient books a slot" in {
      val patientId = "John Doe"

      val scheduled1 = SlotScheduled(SlotId.create, DayId(DoctorId("123"), today), tenAmToday, tenMinutes)
      val scheduled2 =
        SlotScheduled(SlotId.create, DayId(DoctorId("123"), today), tenAmToday.plusMinutes(10), tenMinutes)
      val booked1 = SlotBooked(scheduled1.slotId, patientId)
      val booked2 = SlotBooked(scheduled2.slotId, patientId)

      `given`(scheduled1, scheduled2, booked1, booked2)
      `then` {
        repository.countByPatientAndMonth(patientId, today.getMonth).map(_ shouldEqual 2)
      }
    }

    "decrement the counter every time a slot booking is cancelled" in {
      val patientId = "John Doe"

      val scheduled1 = SlotScheduled(SlotId.create, DayId(DoctorId("123"), today), tenAmToday, tenMinutes)
      val scheduled2 =
        SlotScheduled(SlotId.create, DayId(DoctorId("123"), today), tenAmToday.plusMinutes(10), tenMinutes)
      val booked1              = SlotBooked(scheduled1.slotId, patientId)
      val booked2              = SlotBooked(scheduled2.slotId, patientId)
      val slotBookingCancelled = SlotBookingCancelled(scheduled2.slotId, "I'm healthy!")

      `given`(scheduled1, scheduled2, booked1, booked2, slotBookingCancelled)
      `then` {
        repository.countByPatientAndMonth(patientId, today.getMonth).map(_ shouldEqual 1)
      }
    }

    "send command to cancel a slot if the booking limit was crossed" in {
      val patientId = "John Doe"

      val dayId = DayId(DoctorId("123"), today)

      val scheduled1 = SlotScheduled(SlotId.create, dayId, tenAmToday, tenMinutes)
      val scheduled2 = SlotScheduled(SlotId.create, dayId, scheduled1.startTime.plusMinutes(10), tenMinutes)
      val scheduled3 = SlotScheduled(SlotId.create, dayId, scheduled2.startTime.plusMinutes(10), tenMinutes)
      val scheduled4 = SlotScheduled(SlotId.create, dayId, scheduled3.startTime.plusMinutes(10), tenMinutes)
      val booked1    = SlotBooked(scheduled1.slotId, patientId)
      val booked2    = SlotBooked(scheduled2.slotId, patientId)
      val booked3    = SlotBooked(scheduled3.slotId, patientId)
      val booked4    = SlotBooked(scheduled4.slotId, patientId)

      `given`(scheduled1, scheduled2, scheduled3, scheduled4, booked1, booked2, booked3)
      `when`(booked4)
      `then` { sentCommands =>
        for {
          count <- repository.countByPatientAndMonth(patientId, today.getMonth)
        } yield {
          count shouldEqual 4
          sentCommands shouldEqual Seq(
            CommandEnvelope(
              CancelSlotBooking(booked4.slotId, "Overbooking"),
              CommandMetadata(metadata.correlationId, CausationId.create, DoctorDayId(dayId))
            )
          )
        }
      }
    }
  }
}
