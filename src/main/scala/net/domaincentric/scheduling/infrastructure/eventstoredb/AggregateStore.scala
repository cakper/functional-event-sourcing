package net.domaincentric.scheduling.infrastructure.eventstoredb
import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.Version.`new`
import net.domaincentric.scheduling.application.eventsourcing.{ Aggregate, AggregateId, EventMetadata, Version }
import net.domaincentric.scheduling.domain.aggregate.{ CommandHandler, State }

class AggregateStore(eventStore: EventStore[EventMetadata]) extends eventsourcing.AggregateStore {
  override def commit[C, E, Er, S <: State[S, E]](
      aggregate: Aggregate[C, E, Er, S],
      metadata: EventMetadata
  ): Task[Aggregate[C, E, Er, S]] =
    (aggregate.version match {
      case `new`   => eventStore.createNewStream(aggregate.id.toString, aggregate.changes, metadata)
      case version => eventStore.appendToStream(aggregate.id.toString, aggregate.changes, metadata, version)
    }).map(_ => aggregate.markAsCommitted)

  override def create[C, E, Er, S <: State[S, E]](
      id: AggregateId,
      handler: CommandHandler[C, E, Er, S]
  ): Aggregate[C, E, Er, S] = Aggregate(id, handler.initialState, handler)

  override def reconsititute[C, E, Er, S <: State[S, E]](
      id: AggregateId,
      handler: CommandHandler[C, E, Er, S]
  ): Aggregate[C, E, Er, S] = create(id, handler)
}
