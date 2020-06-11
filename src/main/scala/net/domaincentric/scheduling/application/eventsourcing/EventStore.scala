package net.domaincentric.scheduling.application.eventsourcing

import monix.eval.Task
import monix.reactive.Observable
import net.domaincentric.scheduling.eventsourcing.Event

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
