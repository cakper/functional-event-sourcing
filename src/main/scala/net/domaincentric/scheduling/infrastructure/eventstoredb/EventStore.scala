package net.domaincentric.scheduling.infrastructure.eventstoredb

import cats.implicits._
import com.eventstore.dbclient._
import monix.eval.Task
import monix.reactive.Observable
import net.domaincentric.scheduling.eventsourcing.{ Event, EventEnvelope, EventMetadata, Version, EventStore => BaseEventStore }
import net.domaincentric.scheduling.eventsourcing.Version._

import scala.compat.java8.FutureConverters._
import scala.jdk.CollectionConverters._

class EventStore(val client: StreamsClient, eventSerde: EventSerde) extends BaseEventStore {
  val pageSize = 2048

  override def readFromStream(streamId: String): Observable[EventEnvelope] = {
    Observable
      .fromAsyncStateAction[StreamRevision, Seq[EventEnvelope]] {
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

  override def createNewStream(streamId: String, events: Seq[Event], commandMetadata: EventMetadata): Task[Version] =
    for {
      events <- Task.fromTry(events.toList.traverse(eventSerde.serialize(_, commandMetadata)))
      result <- Task.deferFuture {
        client.appendToStream(streamId, SpecialStreamRevision.NO_STREAM, events.asJava).toScala
      }
    } yield result.getNextExpectedRevision.getValueUnsigned

  override def appendToStream(
      streamId: String,
      events: Seq[Event],
      commandMetadata: EventMetadata,
      expectedVersion: Version
  ): Task[Version] =
    for {
      events <- Task.fromTry(events.toList.traverse(eventSerde.serialize(_, commandMetadata)))
      result <- Task.deferFuture {
        client.appendToStream(streamId, new StreamRevision(expectedVersion), events.asJava).toScala
      }
    } yield result.getNextExpectedRevision.getValueUnsigned
}
