package net.domaincentric.scheduling.domain.aggregate

trait State[S <: State[S, E], E] { this: S =>
  def apply(event: E): S
}
