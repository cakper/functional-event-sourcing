package net.domaincentric.scheduling.domain.aggregate

trait Rules[C, E, Er, S <: State[S, E]] {
  def apply(state: S, command: C): Either[Er, Seq[E]]
  def initialState: S
}

object Rules {
  implicit def eventToResult[E, Er](event: E): Either[Er, Seq[E]]        = Right(Seq(event))
  implicit def eventsToResult[E, Er](events: Seq[E]): Either[Er, Seq[E]] = Right(events)
  implicit def errorToResult[E, Er](error: Er): Either[Er, Seq[E]]       = Left(error)
}
