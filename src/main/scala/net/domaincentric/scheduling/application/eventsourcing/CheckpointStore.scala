package net.domaincentric.scheduling.application.eventsourcing

import com.eventstore.dbclient.proto.streams.StreamsOuterClass.ReadResp.Checkpoint
import monix.eval.Task

trait CheckpointStore {
  def read(subscriptionId: SubscriptionId): Task[Option[Checkpoint]]
  def update(subscriptionId: SubscriptionId, checkpoint: Checkpoint): Task[Unit]
}
