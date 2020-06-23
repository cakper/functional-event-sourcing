package net.domaincentric.scheduling.application.eventsourcing

import monix.eval.Task

trait CheckpointStore {
  def read(subscriptionId: SubscriptionId): Task[Option[Checkpoint]]
  def update(subscriptionId: SubscriptionId, checkpoint: Checkpoint): Task[Unit]
}
