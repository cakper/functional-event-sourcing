package net.domaincentric.scheduling.infrastructure.eventstoredb

import java.time.{ LocalDate, LocalDateTime }
import java.util.UUID

import com.eventstore.dbclient.{ StreamNotFoundException, StreamsClient, Timeouts, UserCredentials }
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import javax.net.ssl.SSLException
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import net.domaincentric.scheduling.application.eventsourcing.{ CausationId, CorrelationId, EventMetadata, Version }
import net.domaincentric.scheduling.domain.writemodel.doctorday.{ DayId, DayScheduled, DoctorId, SlotId, SlotScheduled }
import net.domaincentric.scheduling.domain.service.{ RandomUuidGenerator, UuidGenerator }
import net.domaincentric.scheduling.test
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.duration._

class EventStoreDbSpec extends AsyncWordSpec with Matchers with test.EventStoreDb {
  implicit val uuidGenerator: UuidGenerator = RandomUuidGenerator

  val client = new EventStore(streamsClient, new EventSerde)

  "event store db client" should {
    val today    = LocalDate.now()
    val doctorId = DoctorId("John Done")
    val dayId    = DayId(doctorId, today)
    "create a new stream" in {
      val streamId = "doctorday-" + UUID.randomUUID().toString.split("-").head

      val eventsToWrite = Seq(
        DayScheduled(dayId, doctorId, today),
        SlotScheduled(SlotId.create, dayId, LocalDateTime.now(), 10.minutes)
      )
      val commandMetadata = EventMetadata(CorrelationId.create, CausationId.create)

      (for {
        version    <- client.createNewStream(streamId, eventsToWrite, commandMetadata)
        readEvents <- client.readFromStream(streamId).toListL
      } yield {
        version shouldEqual new Version(1L)
        readEvents.map(_.data) shouldEqual eventsToWrite
      }).runToFuture
    }

    "append to an existing stream" in {
      val streamId = "doctorday-" + UUID.randomUUID().toString.split("-").head

      val firstWrite    = Seq(DayScheduled(dayId, doctorId, today))
      val firstMetadata = EventMetadata(CorrelationId.create, CausationId.create)

      val secondWrite    = Seq(SlotScheduled(SlotId.create, dayId, LocalDateTime.now(), 10.minutes))
      val secondMetadata = EventMetadata(CorrelationId.create, CausationId.create)

      (for {
        firstVersion  <- client.createNewStream(streamId, firstWrite, firstMetadata)
        secondVersion <- client.appendToStream(streamId, secondWrite, secondMetadata, firstVersion)
        readEvents    <- client.readFromStream(streamId).toListL
      } yield {
        firstVersion shouldEqual new Version(0L)
        secondVersion shouldEqual new Version(1L)
        readEvents.map(_.data) shouldEqual firstWrite.appendedAll(secondWrite)
      }).runToFuture
    }

    "truncate" in {
      val streamId = "doctorday-" + RandomUuidGenerator.next().toString

      val eventsToWrite = Seq(
        DayScheduled(dayId, doctorId, today),
        SlotScheduled(SlotId.create, dayId, LocalDateTime.now(), 10.minutes),
        SlotScheduled(SlotId.create, dayId, LocalDateTime.now(), 10.minutes),
        SlotScheduled(SlotId.create, dayId, LocalDateTime.now(), 10.minutes),
        SlotScheduled(SlotId.create, dayId, LocalDateTime.now(), 10.minutes),
        SlotScheduled(SlotId.create, dayId, LocalDateTime.now(), 10.minutes)
      )
      val commandMetadata = EventMetadata(CorrelationId.create, CausationId.create)

      (for {
        _      <- client.createNewStream(streamId, eventsToWrite, commandMetadata)
        _      <- client.truncateStream(streamId, 3L)
        events <- client.readFromStream(streamId).toListL
      } yield {
        events.map(_.data) shouldEqual eventsToWrite.drop(3)
      }).runToFuture
    }

    "soft delete" in {
      val streamId = "doctorday-" + RandomUuidGenerator.next().toString

      val eventsToWrite = Seq(
        DayScheduled(dayId, doctorId, today),
        SlotScheduled(SlotId.create, dayId, LocalDateTime.now(), 10.minutes),
        SlotScheduled(SlotId.create, dayId, LocalDateTime.now(), 10.minutes),
        SlotScheduled(SlotId.create, dayId, LocalDateTime.now(), 10.minutes),
        SlotScheduled(SlotId.create, dayId, LocalDateTime.now(), 10.minutes),
        SlotScheduled(SlotId.create, dayId, LocalDateTime.now(), 10.minutes)
      )
      val commandMetadata = EventMetadata(CorrelationId.create, CausationId.create)

      (for {
        _ <- client.createNewStream(streamId, eventsToWrite, commandMetadata)
        _ <- client.deleteStream(streamId, 5L)
        error <- client.readFromStream(streamId).toListL.onErrorHandleWith {
          case error: StreamNotFoundException => Task.now(error)
        }
      } yield {
        error shouldBe a[StreamNotFoundException]
      }).runToFuture
    }
  }
}
