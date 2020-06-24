package net.domaincentric.scheduling.application.eventsourcing

import monix.eval.Task
import net.domaincentric.scheduling.domain.writemodel.State

trait AggregateStore {
  def commit[C, E, Er, S <: State[S, E]](
      handled: Aggregate[C, E, Er, S],
      metadata: EventMetadata
  ): Task[Aggregate[C, E, Er, S]]

  def reconsititute[C, E, Er, S <: State[S, E]](
      aggregate: Aggregate[C, E, Er, S]
  ): Task[Aggregate[C, E, Er, S]]
}
