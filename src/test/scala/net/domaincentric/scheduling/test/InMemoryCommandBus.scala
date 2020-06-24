package net.domaincentric.scheduling.test

import monix.eval.Task
import monix.reactive.Observable
import net.domaincentric.scheduling.application.eventsourcing.CommandBus.CommandEnvelope
import net.domaincentric.scheduling.application.eventsourcing.{ CommandBus, CommandMetadata, MessageHandler }

class InMemoryCommandBus extends CommandBus {
  private var commands      = List.empty[CommandEnvelope]
  private var monkeyEnabled = false

  override def send(command: Any, metadata: CommandMetadata): Task[Unit] =
    if (monkeyEnabled) Task.raiseError(new RuntimeException("Wonky IO monkey!"))
    else
      Task.now {
        commands = commands.appended(CommandEnvelope(command: Any, metadata: CommandMetadata))
      }

  def reset(): Unit = {
    commands = List.empty
    monkeyEnabled = false
  }

  def enableMonkey(): Unit = {
    monkeyEnabled = true
  }

  def disableMonkey(): Unit = {
    monkeyEnabled = false
  }

  override def subscribe(): Observable[CommandEnvelope] = {
    Observable.fromIterable(commands)
  }
}
