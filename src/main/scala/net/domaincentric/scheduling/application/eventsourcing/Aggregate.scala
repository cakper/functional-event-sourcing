package net.domaincentric.scheduling.application.eventsourcing

import net.domaincentric.scheduling.domain.aggregate.{ CommandHandler, State }

case class Aggregate[C, E, Er, S <: State[S, E]](
    id: AggregateId,
    state: S,
    handler: CommandHandler[C, E, Er, S],
    version: Version = Version.`new`,
    changes: Seq[E] = Seq.empty
) {
  def handle(command: C): Either[Er, Aggregate[C, E, Er, S]] =
    handler(state, command).map { newEvents =>
      copy(state = newEvents.foldLeft(state)(_.apply(_)), changes = changes.appendedAll(newEvents))
    }

  def reconstitute(events: Seq[E]): Aggregate[C, E, Er, S] =
    copy(
      state = events.foldLeft(state)(_.apply(_)),
      version = version.incrementBy(events.length)
    )

  def markAsCommitted: Aggregate[C, E, Er, S] =
    copy(
      version = version.incrementBy(changes.length),
      changes = Seq.empty
    )
}
