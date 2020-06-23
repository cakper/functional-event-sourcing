package net.domaincentric.scheduling.application.eventsourcing

import com.eventstore.dbclient.Position
import monix.eval.Task
import monix.reactive.Observable

trait EventStore[M] {
  def readFromStream(streamId: String): Observable[Envelope[M]]

  def createNewStream(streamId: String, events: Seq[Any], metadata: M): Task[Version]
  def appendToStream(
      streamId: String,
      events: Seq[Any],
      metadata: M,
      expectedVersion: Version
  ): Task[Version]
  def appendToStream(streamId: String, events: Seq[Any], metadata: M): Task[Version]

  def deleteStream(streamId: String, expectedVersion: Version): Task[Unit]

  def subscribeToAll(fromPosition: Position = Position.START): Observable[Envelope[M]]
}

case class OptimisticConcurrencyException(message: String) extends RuntimeException(message)
