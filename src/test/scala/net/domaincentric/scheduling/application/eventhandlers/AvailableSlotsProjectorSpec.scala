package net.domaincentric.scheduling.application.eventhandlers

import java.time.{ Duration => _, _ }
import java.util.UUID

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import net.domaincentric.scheduling.application.eventsourcing.{ EventHandler, EventMetadata }
import net.domaincentric.scheduling.domain.aggregate.doctorday.{ SlotBooked, SlotBookingCancelled, SlotScheduled }
import net.domaincentric.scheduling.domain.readmodel.avialbleslots
import net.domaincentric.scheduling.domain.readmodel.avialbleslots.Repository
import net.domaincentric.scheduling.infrastructure.mongodb.MongodbAvailableSlotsRepository
import org.mongodb.scala.{ MongoClient, MongoDatabase }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{ Assertion, BeforeAndAfterEach }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class AvailableSlotsProjectorSpec extends AsyncWordSpec with Matchers with BeforeAndAfterEach {

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    Await.result(database.drop().toFuture(), Duration.Inf)
  }

  def randomId(): UUID      = UUID.randomUUID()
  implicit val clock: Clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)

  private val today: LocalDate           = LocalDate.now(clock)
  private val tenAm: LocalTime           = LocalTime.of(10, 0)
  private val tenAmToday: LocalDateTime  = LocalDateTime.of(today, tenAm)
  private val tenMinutes: FiniteDuration = 10.minutes
  private val mongoDbClient              = MongoClient("mongodb://localhost")
  private val database: MongoDatabase    = mongoDbClient.getDatabase("projections")

  private val repository: Repository = new MongodbAvailableSlotsRepository(database)
  private val handler: EventHandler  = new AvailableSlotsProjector(repository)

  private val metadata: EventMetadata = EventMetadata("abc", "123")

  def `given`(events: Any*): Unit = {
    Await.result(
      Task
        .traverse(events) { event =>
          handler.handle(event, metadata, UUID.randomUUID(), 0L, Instant.now())
        }
        .runToFuture,
      Duration.Inf
    )
  }

  def `then`[A](actualResultT: Task[A], expected: A): Future[Assertion] =
    actualResultT.map(_ shouldEqual expected).runToFuture

  "available slots projector" should {
    "add slot to the list" in {
      val scheduled = SlotScheduled(randomId(), randomId(), tenAmToday, tenMinutes)
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
      val scheduled = SlotScheduled(randomId(), randomId(), tenAmToday, tenMinutes)
      val booked    = SlotBooked(scheduled.slotId, "John Doe")
      `given`(scheduled, booked)
      `then`(repository.getAvailableSlotsOn(today), Seq.empty)
    }

    "show slot if booking was cancelled" in {
      val scheduled = SlotScheduled(randomId(), randomId(), tenAmToday, tenMinutes)
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
