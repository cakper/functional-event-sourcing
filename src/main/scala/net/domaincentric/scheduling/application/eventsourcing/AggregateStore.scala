package net.domaincentric.scheduling.application.eventsourcing

import monix.eval.Task
import net.domaincentric.scheduling.domain.aggregate.{ CommandHandler, State }

trait AggregateStore {
  def commit[C, E, Er, S <: State[S, E]](
      handled: Aggregate[C, E, Er, S],
      metadata: EventMetadata
  ): Task[Aggregate[C, E, Er, S]]
  def create[C, E, Er, S <: State[S, E]](id: AggregateId, handler: CommandHandler[C, E, Er, S]): Aggregate[C, E, Er, S]
  def reconsititute[C, E, Er, S <: State[S, E]](
      id: AggregateId,
      handler: CommandHandler[C, E, Er, S]
  ): Aggregate[C, E, Er, S]
}
