package net.domaincentric.scheduling.infrastructure.eventstoredb

import monix.execution.Scheduler.Implicits.global
import net.domaincentric.scheduling.application.eventsourcing.{ Checkpoint, SubscriptionId }
import net.domaincentric.scheduling.domain.service.{ RandomUuidGenerator, UuidGenerator }
import net.domaincentric.scheduling.test.EventStoreDb
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class CheckpointStoreSpec extends AsyncWordSpec with Matchers with EventStoreDb {
  implicit val uuidGenerator: UuidGenerator = RandomUuidGenerator
  private val eventStore                    = new EventStore(streamsClient, new CheckpointSerde)

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
