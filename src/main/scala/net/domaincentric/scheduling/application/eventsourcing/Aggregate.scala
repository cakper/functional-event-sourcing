package net.domaincentric.scheduling.application.eventsourcing

import net.domaincentric.scheduling.domain.writemodel.{ Rules, State }

case class Aggregate[C, E, Er, S <: State[S, E]](
    id: AggregateId,
    state: S,
    rules: Rules[C, E, Er, S],
    version: Version = Version.`new`,
    changes: Seq[E] = Seq.empty
) {
  def handle(command: C): Either[Er, Aggregate[C, E, Er, S]] =
    rules(state, command).map { newEvents =>
      copy(state = newEvents.foldLeft(state)(_.apply(_)), changes = changes.appendedAll(newEvents))
    }

  def reconstitute(events: Seq[E]): Aggregate[C, E, Er, S] =
    copy(
      state = events.foldLeft(state)(_.apply(_)),
      version = version.incrementBy(events.length)
    )

  def reconstitute(state: S, version: Version): Aggregate[C, E, Er, S] = copy(state = state, version = version)

  def markAsCommitted: Aggregate[C, E, Er, S] =
    copy(
      version = version.incrementBy(changes.length),
      changes = Seq.empty
    )
}

object Aggregate {
  def apply[C, E, Er, S <: State[S, E]](
      id: AggregateId,
      handler: Rules[C, E, Er, S]
  ): Aggregate[C, E, Er, S] = Aggregate(id, handler.initialState, handler)
}
