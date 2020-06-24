package net.domaincentric.scheduling.infrastructure.eventstoredb

import monix.execution.Scheduler.Implicits.global
import net.domaincentric.scheduling.application.eventsourcing.CommandBus.CommandEnvelope
import net.domaincentric.scheduling.application.eventsourcing.{ AggregateId, CausationId, CommandMetadata, CorrelationId }
import net.domaincentric.scheduling.domain.service.{ RandomUuidGenerator, UuidGenerator }
import net.domaincentric.scheduling.domain.writemodel.doctorday.{ CancelSlotBooking, SlotId }
import net.domaincentric.scheduling.test.EventStoreDb
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class CommandBusSpec extends AsyncWordSpec with Matchers with EventStoreDb {
  implicit val uuidGenerator: UuidGenerator = RandomUuidGenerator

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
