package net.domaincentric.scheduling.infrastructure.inmemory

import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.{ AggregateId, EventMetadata, MessageEnvelope }

class ColdStorage extends eventsourcing.ColdStorage {
  var events: Seq[MessageEnvelope[EventMetadata]] = Seq.empty

  override def saveAll(aggregateId: AggregateId, events: Seq[MessageEnvelope[EventMetadata]]): Task[Unit] = {
    Task.eval(this.events = events)
  }
}
