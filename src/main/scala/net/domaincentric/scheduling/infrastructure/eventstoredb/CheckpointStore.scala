package net.domaincentric.scheduling.infrastructure.eventstoredb

import com.eventstore.dbclient.StreamNotFoundException
import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.{ Checkpoint, SubscriptionId }

class CheckpointStore(eventStore: eventsourcing.EventStore[Unit]) extends eventsourcing.CheckpointStore {
  override def read(subscriptionId: SubscriptionId): Task[Option[Checkpoint]] =
    eventStore
      .readLastFromStream("subscription-" + subscriptionId.value)
      .map(_.map(_.data).collect {
        case c: Checkpoint => c
      })
      .onErrorHandleWith {
        case _: StreamNotFoundException => Task.now(None)
      }

  override def update(subscriptionId: SubscriptionId, checkpoint: Checkpoint): Task[Unit] =
    eventStore.appendToStream("subscription-" + subscriptionId.value, Seq(checkpoint), ()).map(_ => ())
}
