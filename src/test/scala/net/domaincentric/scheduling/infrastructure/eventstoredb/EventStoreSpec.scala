package net.domaincentric.scheduling.infrastructure.eventstoredb

import java.time.{ LocalDate, LocalDateTime }
import java.util.UUID

import com.eventstore.dbclient.{ StreamsClient, Timeouts, UserCredentials }
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import javax.net.ssl.SSLException
import monix.execution.Scheduler.Implicits.global
import net.domaincentric.scheduling.application.eventsourcing.{ EventMetadata, Version }
import net.domaincentric.scheduling.domain.aggregate.doctorday.{ Scheduled, SlotScheduled }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.duration._

class EventStoreSpec extends AsyncWordSpec with Matchers {
  val streamsClient: StreamsClient = {
    val creds = new UserCredentials("admin", "changeit")
    new StreamsClient("localhost", 2113, creds, Timeouts.DEFAULT, getClientSslContext)
  }

  private def getClientSslContext =
    try GrpcSslContexts.forClient.trustManager(InsecureTrustManagerFactory.INSTANCE).build
    catch {
      case _: SSLException => null
    }

  val client = new EventStore(streamsClient, new EventSerde)

  "event store db client" should {
    "create a new stream" in {
      val streamId = "doctorday-" + UUID.randomUUID().toString.split("-").head

      val eventsToWrite = Seq(
        Scheduled(UUID.randomUUID(), "John Done", LocalDate.now()),
        SlotScheduled(UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), 10.minutes)
      )
      val commandMetadata = EventMetadata("abc", "123")

      (for {
        version    <- client.createNewStream(streamId, eventsToWrite, commandMetadata)
        readEvents <- client.readFromStream(streamId).toListL
      } yield {
        version shouldEqual new Version(1L)
        readEvents.map(_.event) shouldEqual eventsToWrite
      }).runToFuture
    }

    "append to an existing stream" in {
      val streamId = "doctorday-" + UUID.randomUUID().toString.split("-").head

      val firstWrite    = Seq(Scheduled(UUID.randomUUID(), "John Done", LocalDate.now()))
      val firstMetadata = EventMetadata("abc", "123")

      val secondWrite    = Seq(SlotScheduled(UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), 10.minutes))
      val secondMetadata = EventMetadata("def", "456")

      (for {
        firstVersion  <- client.createNewStream(streamId, firstWrite, firstMetadata)
        secondVersion <- client.appendToStream(streamId, secondWrite, secondMetadata, firstVersion)
        readEvents    <- client.readFromStream(streamId).toListL
      } yield {
        firstVersion shouldEqual new Version(0L)
        secondVersion shouldEqual new Version(1L)
        readEvents.map(_.event) shouldEqual firstWrite.appendedAll(secondWrite)
      }).runToFuture
    }
  }
}
