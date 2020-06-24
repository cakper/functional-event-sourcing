package net.domaincentric.scheduling.infrastructure.eventstoredb
import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.SnapshotStore.SnapshotEnvelope
import net.domaincentric.scheduling.application.eventsourcing.Version.`new`
import net.domaincentric.scheduling.application.eventsourcing.{ Aggregate, EventMetadata, OptimisticConcurrencyException, SnapshotMetadata }
import net.domaincentric.scheduling.domain.writemodel.State

object StreamAlreadyExists extends RuntimeException

class AggregateStore(
    eventStore: EventStore[EventMetadata],
    snapshotStore: SnapshotStore,
    snapshotThreshold: Option[Int] = None
) extends eventsourcing.AggregateStore {
  override def commit[C, E, Er, S <: State[S, E]](
      aggregate: Aggregate[C, E, Er, S],
      metadata: EventMetadata
  ): Task[Aggregate[C, E, Er, S]] =
    for {
      _ <- aggregate.version match {
        case `new` =>
          eventStore.createNewStream(aggregate.id.toString, aggregate.changes, metadata).onErrorRecoverWith {
            case _: OptimisticConcurrencyException => Task.raiseError(StreamAlreadyExists)
          }
        case version => eventStore.appendToStream(aggregate.id.toString, aggregate.changes, metadata, version)
      }
      committed <- Task.now(aggregate.markAsCommitted)
      _ <- snapshotThreshold match {
        case Some(threshold) if aggregate.changes.length >= threshold =>
          snapshotStore.write(
            committed.id,
            committed.state,
            SnapshotMetadata(metadata.correlationId, metadata.causationId, committed.version)
          )
        case _ => Task.unit
      }
    } yield committed

  override def reconsititute[C, E, Er, S <: State[S, E]](
      aggregate: Aggregate[C, E, Er, S]
  ): Task[Aggregate[C, E, Er, S]] =
    for {
      withSnapshot <- snapshotStore.read(aggregate.id).map {
        case Some(SnapshotEnvelope(snapshot, metadata)) =>
          snapshot match {
            case s: S => aggregate.reconstitute(s, metadata.version)
            case _    => aggregate // Todo: log error
          }
        case _ => aggregate
      }
      withEvents <- eventStore
        .readFromStream(withSnapshot.id.toString, withSnapshot.version.nextReadVersion)
        .toListL
        .map { envelopes =>
          withSnapshot.reconstitute(envelopes.map(_.data).collect {
            case e: E => e
          })
        }
    } yield withEvents
}
