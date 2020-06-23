package net.domaincentric.scheduling.infrastructure.eventstoredb

import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.{ Checkpoint, SubscriptionId }

class CheckpointStore(eventStore: eventsourcing.EventStore[Unit]) extends eventsourcing.CheckpointStore {
  override def read(subscriptionId: SubscriptionId): Task[Option[Checkpoint]] =
    eventStore
      .readLastFromStream(subscriptionId.value)
      .map(_.map(_.data).collect {
        case c: Checkpoint => c
      })
  override def update(subscriptionId: SubscriptionId, checkpoint: Checkpoint): Task[Unit] =
    eventStore.appendToStream(subscriptionId.value, Seq(checkpoint), ()).map(_ => ())
}
