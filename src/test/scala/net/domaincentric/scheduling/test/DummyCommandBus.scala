package net.domaincentric.scheduling.test

import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing.{ CommandBus, CommandMetadata }
import net.domaincentric.scheduling.test.DummyCommandBus.SentCommand

class DummyCommandBus extends CommandBus {
  private var commands = List.empty[SentCommand]
  override def send(aggregateId: String, command: Any, metadata: CommandMetadata): Task[Unit] = Task.now {
    commands = commands.appended(SentCommand(aggregateId: String, command: Any, metadata: CommandMetadata))
  }

  def reset(): Unit = {
    commands = List.empty
  }

  def sentCommands: Seq[Any] = commands
}

object DummyCommandBus {
  case class SentCommand(aggregateId: String, command: Any, metadata: CommandMetadata)
}
