package net.domaincentric.scheduling.application.eventsourcing

import com.eventstore.dbclient.Position
import monix.eval.Task
import monix.reactive.Observable

trait EventStore {
  def readFromStream(streamId: String): Observable[EventEnvelope]
  def createNewStream(streamId: String, events: Seq[Any], commandMetadata: EventMetadata): Task[Version]
  def appendToStream(
      streamId: String,
      events: Seq[Any],
      commandMetadata: EventMetadata,
      expectedVersion: Version
  ): Task[Version]
  def subscribeToAll(fromPosition: Position = Position.START): Observable[EventEnvelope]
}
