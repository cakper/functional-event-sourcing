package net.domaincentric.scheduling.infrastructure.eventstoredb

import com.eventstore.dbclient.StreamNotFoundException
import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.SnapshotStore.SnapshotEnvelope
import net.domaincentric.scheduling.application.eventsourcing.{ AggregateId, SnapshotMetadata, SnapshotStore, Version }

class SnapshotStore(eventStore: EventStore[SnapshotMetadata]) extends eventsourcing.SnapshotStore {
  override def read(aggregateId: AggregateId): Task[Option[SnapshotStore.SnapshotEnvelope]] =
    eventStore
      .readFromStream(s"snapshot-$aggregateId")
      .toListL
      .map(_.lastOption.map { result =>
        SnapshotEnvelope(result.data, result.metadata)
      })
      .onErrorHandleWith {
        case _: StreamNotFoundException => Task.now(None)
      }
  // TODO: Optimise read

  override def write(aggregateId: AggregateId, snapshot: Any, snapshotMetadata: SnapshotMetadata): Task[Version] =
    eventStore.appendToStream(
      s"snapshot-$aggregateId",
      Seq(snapshot),
      snapshotMetadata
    )
}
