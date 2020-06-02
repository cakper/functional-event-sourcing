package net.domaincentric.scheduling.eventsourcing

trait State[S <: State[S, E], E <: Event] { this: S =>
  def apply(events: Seq[E]): S = events.foldLeft(this)(_.apply(_))
  def apply(event: E): S
}
