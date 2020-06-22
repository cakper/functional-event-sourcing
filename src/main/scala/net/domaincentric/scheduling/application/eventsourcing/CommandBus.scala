package net.domaincentric.scheduling.application.eventsourcing

import monix.eval.Task
import monix.reactive.Observable
import net.domaincentric.scheduling.application.eventsourcing.CommandBus.CommandEnvelope

trait CommandBus {
  def send(command: Any, metadata: CommandMetadata): Task[Unit]
  def subscribe(): Observable[CommandEnvelope]
}

object CommandBus {
  case class CommandEnvelope(command: Any, metadata: CommandMetadata)
}
