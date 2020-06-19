package net.domaincentric.scheduling.application.eventsourcing

import monix.eval.Task

trait CommandBus {
  def send(aggregateId: String, command: Any, metadata: CommandMetadata): Task[Unit]
}
