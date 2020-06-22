package net.domaincentric.scheduling.infrastructure.eventstoredb

import java.util.UUID

import cats.implicits._
import com.eventstore.dbclient._
import monix.eval.Task
import monix.reactive.Observable
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.Version._
import net.domaincentric.scheduling.application.eventsourcing.{ Envelope, EventMetadata, Version }

import scala.compat.java8.FutureConverters._
import scala.jdk.CollectionConverters._

class EventStore[M](val client: StreamsClient, eventSerde: Serde[M]) extends eventsourcing.EventStore[M] {
  def truncateStreamBefore(streamId: String, version: Version): Task[Unit] = {
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
                ("{\"$tb\":" + version.value + "}").getBytes,
                "{}".getBytes
              )
            ).asJava
          )
          .toScala
      }
      .map(_ => ())
  }

  private val pageSize = 4096
//  private val subscriptionFilter: SubscriptionFilter =
//    new SubscriptionFilterBuilder().withEventTypePrefix(eventSerde.prefix).build()

  override def readFromStream(streamId: String): Observable[Envelope[M]] = {
    Observable
      .fromAsyncStateAction[StreamRevision, Seq[Envelope[M]]] {
        case StreamRevision.END => Task.now((Seq.empty, StreamRevision.END))
        case revision =>
          for {
            result <- Task.deferFuture(
              client.readStream(Direction.Forward, streamId, revision, pageSize, false).toScala
            )
            events <- Task.fromTry(result.getEvents.asScala.toList.traverse(eventSerde.deserialize))
          } yield {
            if (events.size < pageSize) (events, StreamRevision.END)
            else (events, new StreamRevision(revision.getValueUnsigned + events.size))
          }
      }(StreamRevision.START)
      .takeWhile(_.nonEmpty)
      .flatMap(Observable.fromIterable)
  }

  override def createNewStream(
      streamId: String,
      events: Seq[Any],
      metadata: M
  ): Task[Version] =
    for {
      events <- serialise(events, metadata)
      result <- Task.deferFuture {
        client.appendToStream(streamId, SpecialStreamRevision.NO_STREAM, events.asJava).toScala
      }
    } yield result.getNextExpectedRevision.getValueUnsigned

  override def subscribeToAll(fromPosition: Position = Position.START): Observable[Envelope[M]] = {
    SubscriptionObservable(
      client.subscribeToAll(fromPosition, false, _)
    ).mapEval { resolvedEvent =>
      Task.fromTry(eventSerde.deserialize(resolvedEvent))
    }
  }

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
}
