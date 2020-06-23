package net.domaincentric.scheduling.infrastructure.eventstoredb

import com.eventstore.dbclient.{ StreamsClient, Timeouts, UserCredentials }
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import javax.net.ssl.SSLException
import monix.execution.Scheduler.Implicits.global
import net.domaincentric.scheduling.application.eventsourcing.{ Checkpoint, SubscriptionId }
import net.domaincentric.scheduling.domain.service.{ RandomUuidGenerator, UuidGenerator }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class CheckpointStoreSpec extends AsyncWordSpec with Matchers {
  implicit val uuidGenerator: UuidGenerator = RandomUuidGenerator

  val streamsClient: StreamsClient = {
    val creds = new UserCredentials("admin", "changeit")
    new StreamsClient("localhost", 2113, creds, Timeouts.DEFAULT, getClientSslContext)
  }

  private def getClientSslContext =
    try GrpcSslContexts.forClient.trustManager(InsecureTrustManagerFactory.INSTANCE).build
    catch {
      case _: SSLException => null
    }

  private val eventStore = new EventStore(streamsClient, new CheckpointSerde)

  val store = new CheckpointStore(eventStore)

  "event store checkpoint store" should {
    "read and write checkpoint to a stream" in {
      val checkpoint = Checkpoint(10)
      (for {
        _      <- store.update(SubscriptionId("test1"), checkpoint)
        result <- store.read(SubscriptionId("test1"))
      } yield {
        result shouldEqual Some(checkpoint)
      }).runToFuture
    }
  }
}
