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

  def readFromStream(streamId: String, fromVersion: Version = Version(0L)): Observable[Envelope[M]]
  def readLastFromStream(streamId: String): Task[Option[Envelope[M]]]

  def deleteStream(streamId: String, expectedVersion: Version): Task[Unit]
  def truncateStream(streamId: String, beforeVersion: Version): Task[Unit]

  def subscribeToAll(fromCheckpoint: Option[Checkpoint] = None): Observable[Envelope[M]]
  def subscribeToCategory(category: String, fromCheckpoint: Option[Checkpoint] = None): Observable[Envelope[M]]
}

case class OptimisticConcurrencyException(message: String) extends RuntimeException(message)
