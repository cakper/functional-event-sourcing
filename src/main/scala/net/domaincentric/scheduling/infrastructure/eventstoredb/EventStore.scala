package net.domaincentric.scheduling.infrastructure.eventstoredb

import java.util.UUID

import cats.implicits._
import com.eventstore.dbclient._
import monix.eval.Task
import monix.reactive.Observable
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.Version._
import net.domaincentric.scheduling.application.eventsourcing.{ Checkpoint, Envelope, OptimisticConcurrencyException, Version }

import scala.compat.java8.FutureConverters._
import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success }

class EventStore[M](val client: StreamsClient, eventSerde: Serde[M]) extends eventsourcing.EventStore[M] {
  private val pageSize = 4096
//  private val subscriptionFilter: SubscriptionFilter =
//    new SubscriptionFilterBuilder().withEventTypePrefix(eventSerde.prefix).build()

  override def readFromStream(streamId: String, fromVersion: Version = Version(0)): Observable[Envelope[M]] = {
    Observable
      .fromAsyncStateAction[StreamRevision, Seq[Envelope[M]]] {
        case StreamRevision.END => Task.now((Seq.empty, StreamRevision.END))
        case revision =>
          for {
            result <- Task.deferFuture(
              client.readStream(Direction.Forward, streamId, revision, pageSize, false).toScala
            )
//            events <- Task.fromTry(result.getEvents.asScala.toList.traverse(eventSerde.deserialize))
            events <- Task.now(result.getEvents.asScala.toList.map(eventSerde.deserialize(_).get)) // TODO: Handle deserialization errors in a better way
          } yield {
            if (events.size < pageSize) (events, StreamRevision.END)
            else (events, new StreamRevision(revision.getValueUnsigned + events.size))
          }
      }(new StreamRevision(fromVersion.value))
      .takeWhile(_.nonEmpty)
      .flatMap(Observable.fromIterable)
  }

  override def readLastFromStream(streamId: String): Task[Option[Envelope[M]]] =
    Task
      .deferFuture(
        client.readStream(Direction.Backward, streamId, StreamRevision.END, 1, false).toScala
      )
      .map(_.getEvents.asScala.toList.headOption.map(eventSerde.deserialize(_).get))

  override def createNewStream(
      streamId: String,
      events: Seq[Any],
      metadata: M
  ): Task[Version] =
    for {
      events <- serialise(events, metadata)
      result <- Task
        .deferFuture {
          client.appendToStream(streamId, SpecialStreamRevision.NO_STREAM, events.asJava).toScala
        }
        .onErrorHandleWith {
          case _: WrongExpectedVersionException =>
            Task.raiseError(
              OptimisticConcurrencyException(
                s"Unable to create a new stream ${streamId}, check if stream already exists"
              )
            )
        }
    } yield result.getNextExpectedRevision.getValueUnsigned

  override def appendToStream(
      streamId: String,
      events: Seq[Any],
      metadata: M,
      expectedVersion: Version
  ): Task[Version] =
    for {
      events <- serialise(events, metadata)
      result <- Task.deferFuture {
        client.appendToStream(streamId, new StreamRevision(expectedVersion), events.asJava).toScala
      }
    } yield result.getNextExpectedRevision.getValueUnsigned

  override def appendToStream(streamId: String, events: Seq[Any], metadata: M): Task[Version] =
    for {
      events <- serialise(events, metadata)
      result <- Task.deferFuture {
        client.appendToStream(streamId, SpecialStreamRevision.ANY, events.asJava).toScala
      }
    } yield result.getNextExpectedRevision.getValueUnsigned

  private def serialise(events: Seq[Any], metadata: M): Task[List[ProposedEvent]] = {
    Task.fromTry(events.toList.traverse(eventSerde.serialize(_, metadata)))
  }

  override def deleteStream(streamId: String, expectedVersion: Version): Task[Unit] =
    Task
      .deferFuture {
        client
          .softDelete(
            streamId,
            new StreamRevision(expectedVersion)
          )
          .toScala
      }
      .map(_ => ())

  override def truncateStream(streamId: String, beforeVersion: Version): Task[Unit] = {
    Task
      .deferFuture {
        client
          .appendToStream(
            "$$" + streamId,
            SpecialStreamRevision.ANY,
            Seq(
              new ProposedEvent(
                UUID.randomUUID(),
                "$metadata",
                "application/json",
                ("{\"$tb\":" + beforeVersion.value + "}").getBytes,
                "{}".getBytes
              )
            ).asJava
          )
          .toScala
      }
      .map(_ => ())
  }

  override def subscribeToAll(fromCheckpoint: Option[Checkpoint] = None): Observable[Envelope[M]] = {
    SubscriptionObservable(
      client.subscribeToAll(fromCheckpoint.map(x => new Position(x.value, x.value)).getOrElse(Position.START), false, _)
    ).flatMap { resolvedEvent =>
      eventSerde.deserialize(resolvedEvent) match {
        case Success(value) => Observable.now(value)
        case Failure(_)     => Observable.empty
      }
    }
    //    TODO: Handle deserialization errors
  }

  override def subscribeToCategory(category: String, fromCheckpoint: Option[Checkpoint]): Observable[Envelope[M]] = {
    SubscriptionObservable(
      client
        .subscribeToStream(
          "$ce-" + category,
          fromCheckpoint.map(c => new StreamRevision(c.value)).getOrElse(StreamRevision.START),
          true,
          _
        )
    ).flatMap { resolvedEvent =>
      eventSerde.deserialize(resolvedEvent) match {
        case Success(value) => Observable.now(value)
        case Failure(_)     => Observable.empty
      }
    }
    //    TODO: Handle deserialization errors
  }
}
