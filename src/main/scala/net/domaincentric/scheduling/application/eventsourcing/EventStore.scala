package net.domaincentric.scheduling.application.eventsourcing

import monix.eval.Task
import monix.reactive.Observable

trait EventStore[M] {
  def createNewStream(streamId: String, events: Seq[Any], metadata: M): Task[Version]
  def appendToStream(
      streamId: String,
      events: Seq[Any],
      metadata: M,
      expectedVersion: Version
  ): Task[Version]
  def appendToStream(streamId: String, events: Seq[Any], metadata: M): Task[Version]

  def readFromStream(streamId: String, fromVersion: Version = Version(0L)): Observable[MessageEnvelope[M]]
  def readLastFromStream(streamId: String): Task[Option[MessageEnvelope[M]]]

  def deleteStream(streamId: String, expectedVersion: Version): Task[Unit]
  def truncateStream(streamId: String, beforeVersion: Version): Task[Unit]

  def subscribeToAll(fromCheckpoint: Option[Checkpoint] = None): Observable[MessageEnvelope[M]]
  def subscribeToStream(stream: String, fromCheckpoint: Option[Checkpoint] = None): Observable[MessageEnvelope[M]]
}

case class OptimisticConcurrencyException(message: String) extends RuntimeException(message)
