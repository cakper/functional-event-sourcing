package net.domaincentric.scheduling.application.messagehandlers

import java.time.{ LocalDate, LocalDateTime, LocalTime }

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import net.domaincentric.scheduling.application.eventsourcing.CommandBus.CommandEnvelope
import net.domaincentric.scheduling.application.eventsourcing._
import net.domaincentric.scheduling.domain.writemodel.calendar.CalendarDayStarted
import net.domaincentric.scheduling.domain.writemodel.doctorday._
import net.domaincentric.scheduling.infrastructure.eventstoredb.{ EventSerde, EventStore }
import net.domaincentric.scheduling.infrastructure.inmemory.ColdStorage
import net.domaincentric.scheduling.infrastructure.mongodb.MongodbArchivableDaysRepository
import net.domaincentric.scheduling.test.{ EventHandlerSpec, EventStoreDb, MongoDb }

import scala.concurrent.duration._

class DayArchiverProcessManagerSpec extends EventHandlerSpec with MongoDb with EventStoreDb {
  private val eventStore  = new EventStore(streamsClient, new EventSerde)
  private val coldStorage = new ColdStorage
  private val repository  = new MongodbArchivableDaysRepository(database)

  override def handler: MessageHandler[EventMetadata] =
    new DayArchiverProcessManager(coldStorage, eventStore, repository, commandBus)

  "day archiver" should {
    "archive all events and truncate them except the last one" in {

      val today: LocalDate           = LocalDate.now(clock)
      val tenAm: LocalTime           = LocalTime.of(10, 0)
      val tenAmToday: LocalDateTime  = LocalDateTime.of(today, tenAm)
      val tenMinutes: FiniteDuration = 10.minutes

      val patientId   = PatientId("John Doe")
      val dayId       = DayId(DoctorId(uuidGenerator.next().toString), today)
      val aggregateId = DoctorDayId(dayId)

      val scheduled = SlotScheduled(SlotId.create, dayId, tenAmToday, tenMinutes)
      val booked    = SlotBooked(scheduled.slotId, patientId)
      val archived  = DayScheduleArchived(dayId)

      val events = Seq(scheduled, booked, archived)

      eventStore
        .createNewStream(
          aggregateId.toString,
          events,
          EventMetadata(CorrelationId.create, CausationId.create)
        )
        .runSyncUnsafe(5.seconds)

      `when`(archived)
      `then`(
        eventStore.readFromStream(aggregateId.toString).toListL.map { presentEvents =>
          coldStorage.events.map(_.data) shouldEqual events
          presentEvents.map(_.data) shouldEqual Seq(archived)
        }
      )
    }

    "send archive command for all slots completed 180 days ago" in {
      val doctorId     = DoctorId("abc")
      val date         = LocalDate.now(clock).minusDays(180)
      val dayId        = DayId(doctorId, date)
      val dayScheduled = DayScheduled(dayId, doctorId, date)
      val dayStarted   = CalendarDayStarted(LocalDate.now(clock))

      `given`(dayScheduled)
      `when`(dayStarted)
      `then` { commands =>
        Task.now {
          commands shouldEqual List(
            CommandEnvelope(
              Archive(),
              CommandMetadata(metadata.correlationId, CausationId.create, DoctorDayId(dayId))
            )
          )
        }
      }
    }
  }
}
