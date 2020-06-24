package net.domaincentric.scheduling.infrastructure.eventstoredb

import com.eventstore.dbclient.{ StreamsClient, Timeouts, UserCredentials }
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import javax.net.ssl.SSLException
import monix.execution.Scheduler.Implicits.global
import net.domaincentric.scheduling.application.eventsourcing.CommandBus.CommandEnvelope
import net.domaincentric.scheduling.application.eventsourcing.{ AggregateId, CausationId, CommandMetadata, CorrelationId }
import net.domaincentric.scheduling.domain.writemodel.doctorday.{ CancelSlotBooking, SlotId }
import net.domaincentric.scheduling.domain.service.{ RandomUuidGenerator, UuidGenerator }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class CommandBusSpec extends AsyncWordSpec with Matchers {
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

  private val eventStore = new EventStore(streamsClient, new CommandSerde)

  val commandBus = new CommandBus(streamsClient, s"commands-${RandomUuidGenerator.next()}")

  "event store command bus" should {
    "read and write commands to a stream" in {
      val booking  = CancelSlotBooking(SlotId.create(RandomUuidGenerator), "test")
      val metadata = CommandMetadata(CorrelationId.create, CausationId.create, AggregateId("abc", "test"))
      (for {
        _      <- commandBus.send(booking, metadata)
        result <- commandBus.subscribe().take(1).toListL
      } yield {
        result shouldEqual List(CommandEnvelope(booking, metadata))
      }).runToFuture
    }
  }
}
