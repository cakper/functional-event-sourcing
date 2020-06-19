package net.domaincentric.scheduling.application.eventhandlers

import java.time.{ Duration => _, _ }

import net.domaincentric.scheduling.application.eventsourcing.EventHandler
import net.domaincentric.scheduling.domain.aggregate.doctorday._
import net.domaincentric.scheduling.domain.readmodel.avialbleslots
import net.domaincentric.scheduling.domain.readmodel.avialbleslots.Repository
import net.domaincentric.scheduling.domain.service.{ RandomUuidGenerator, UuidGenerator }
import net.domaincentric.scheduling.infrastructure.mongodb.MongodbAvailableSlotsRepository
import org.mongodb.scala.{ MongoClient, MongoDatabase }
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Await
import scala.concurrent.duration._

class AvailableSlotsProjectorSpec extends ProjectorSpec with BeforeAndAfterEach {
  private val database: MongoDatabase = MongoClient("mongodb://localhost").getDatabase("projections")

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    Await.result(database.drop().toFuture(), Duration.Inf)
  }

  implicit val uuidGenerator: UuidGenerator = RandomUuidGenerator
  implicit val clock: Clock                 = Clock.fixed(Instant.now(), ZoneOffset.UTC)

  val today: LocalDate           = LocalDate.now(clock)
  val tenAm: LocalTime           = LocalTime.of(10, 0)
  val tenAmToday: LocalDateTime  = LocalDateTime.of(today, tenAm)
  val tenMinutes: FiniteDuration = 10.minutes

  val repository: Repository = new MongodbAvailableSlotsRepository(database)
  val handler: EventHandler  = new AvailableSlotsProjector(repository)

  "available slots projector" should {
    "add slot to the list" in {
      val scheduled = SlotScheduled(SlotId.create, DayId.create, tenAmToday, tenMinutes)
      `given`(scheduled)
      `then`(
        repository.getAvailableSlotsOn(today),
        Seq(
          avialbleslots.AvailableSlot(
            scheduled.dayId,
            scheduled.slotId,
            scheduled.startTime.toLocalDate,
            scheduled.startTime.toLocalTime,
            scheduled.duration.toString()
          )
        )
      )
    }

    "hide slot from the list if it was booked" in {
      val scheduled = SlotScheduled(SlotId.create, DayId.create, tenAmToday, tenMinutes)
      val booked    = SlotBooked(scheduled.slotId, "John Doe")
      `given`(scheduled, booked)
      `then`(repository.getAvailableSlotsOn(today), Seq.empty)
    }

    "show slot if booking was cancelled" in {
      val scheduled = SlotScheduled(SlotId.create, DayId.create, tenAmToday, tenMinutes)
      val booked    = SlotBooked(scheduled.slotId, "John Doe")
      val cancelled = SlotBookingCancelled(scheduled.slotId, "Can't make it")
      `given`(scheduled, booked, cancelled)
      `then`(
        repository.getAvailableSlotsOn(today),
        Seq(
          avialbleslots.AvailableSlot(
            scheduled.dayId,
            scheduled.slotId,
            scheduled.startTime.toLocalDate,
            scheduled.startTime.toLocalTime,
            scheduled.duration.toString()
          )
        )
      )
    }
  }
}
