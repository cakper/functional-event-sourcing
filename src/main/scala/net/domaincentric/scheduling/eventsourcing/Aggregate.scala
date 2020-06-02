package net.domaincentric.scheduling.eventsourcing

import net.domaincentric.scheduling.eventsourcing.Aggregate.Handler

case class Aggregate[C <: Command, E <: Event, Er <: Error, S <: State[S, E]](
    id: String,
    state: S,
    handler: Handler[C, E, Er, S],
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

object Aggregate {
  type Handler[C <: Command, E <: Event, Er <: Error, S <: State[S, E]] = (S, C) => Either[Er, Seq[E]]

  implicit def eventToResult[E <: Event, Er <: Error](event: E): Either[Er, Seq[E]] = Right(Seq(event))

  implicit def eventsToResult[E <: Event, Er <: Error](events: Seq[E]): Either[Er, Seq[E]] = Right(events)

  implicit def errorToResult[E <: Event, Er <: Error](error: Er): Either[Er, Seq[E]] = Left(error)
}
