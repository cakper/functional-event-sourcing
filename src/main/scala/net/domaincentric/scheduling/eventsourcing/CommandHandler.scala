package net.domaincentric.scheduling.eventsourcing

trait CommandHandler[C <: Command, E <: Event, Er <: Error, S <: State[S, E]] {
  def apply(state: S, command: C): Either[Er, Seq[E]]
}

object CommandHandler {
  implicit def eventToResult[E <: Event, Er <: Error](event: E): Either[Er, Seq[E]]        = Right(Seq(event))
  implicit def eventsToResult[E <: Event, Er <: Error](events: Seq[E]): Either[Er, Seq[E]] = Right(events)
  implicit def errorToResult[E <: Event, Er <: Error](error: Er): Either[Er, Seq[E]]       = Left(error)
}
