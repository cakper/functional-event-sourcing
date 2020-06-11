package net.domaincentric.scheduling.application.eventsourcing

import net.domaincentric.scheduling.eventsourcing.{ Command, CommandHandler, Error, Event, State }

case class Aggregate[C <: Command, E <: Event, Er <: Error, S <: State[S, E]](
    id: String,
    state: S,
    handler: CommandHandler[C, E, Er, S],
    version: Version = Version.`new`,
    changes: Seq[E] = Seq.empty
) {
  def handle(command: C): Either[Er, Aggregate[C, E, Er, S]] =
    handler(state, command).map { newEvents =>
      copy(state = state.apply(newEvents), changes = changes.appendedAll(newEvents))
    }

  def reconstitute(events: Seq[E]): Aggregate[C, E, Er, S] =
    copy(
      state = state.apply(events),
      version = version.incrementBy(events.length)
    )

  def markAsCommitted: Aggregate[C, E, Er, S] =
    copy(
      version = version.incrementBy(changes.length),
      changes = Seq.empty
    )
}
