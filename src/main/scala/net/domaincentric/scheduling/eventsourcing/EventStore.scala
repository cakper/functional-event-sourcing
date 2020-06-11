package net.domaincentric.scheduling.eventsourcing

import monix.eval.Task
import monix.reactive.Observable

trait EventStore {
  def readFromStream(streamId: String): Observable[EventEnvelope]
  def createNewStream(streamId: String, events: Seq[Event], commandMetadata: EventMetadata): Task[Version]
  def appendToStream(
      streamId: String,
      events: Seq[Event],
      commandMetadata: EventMetadata,
      expectedVersion: Version
  ): Task[Version]
}
