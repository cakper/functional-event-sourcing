package net.domaincentric.scheduling.application.eventsourcing

import monix.eval.Task

trait ColdStorage {
  def saveAll(aggregateId: AggregateId, events: Seq[MessageEnvelope[EventMetadata]]): Task[Unit]
}
