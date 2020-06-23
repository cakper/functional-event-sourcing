package net.domaincentric.scheduling.application.eventsourcing

import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing.SnapshotStore.SnapshotEnvelope

trait SnapshotStore {
  def read(aggregateId: AggregateId): Task[Option[SnapshotEnvelope]]
  def write(
      aggregateId: AggregateId,
      snapshot: Any,
      snapshotMetadata: SnapshotMetadata
  ): Task[Version]
}

object SnapshotStore {
  case class SnapshotEnvelope(snapshot: Any, metadata: SnapshotMetadata)
}
