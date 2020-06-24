package net.domaincentric.scheduling.domain.writemodel

trait State[S <: State[S, E], E] { this: S =>
  def apply(event: E): S
}
