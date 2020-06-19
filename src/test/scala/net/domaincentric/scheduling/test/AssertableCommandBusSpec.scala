package net.domaincentric.scheduling.test

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import net.domaincentric.scheduling.application.eventsourcing.{ CommandBus, CommandMetadata }
import net.domaincentric.scheduling.test.AssertableCommandBus.SentCommand
import org.scalatest.Assertion

import scala.concurrent.Future

trait AssertableCommandBusSpec {
  this: EventHandlerSpec =>
  private val assertableCommandBus: AssertableCommandBus = new AssertableCommandBus()
  val commandBus: CommandBus                             = assertableCommandBus

  def `then`[A](assertionT: Seq[SentCommand] => Task[Assertion]): Future[Assertion] = {
    uuidGenerator.replay()
    assertionT(assertableCommandBus.sentCommands).runToFuture.map { assertion =>
      uuidGenerator.reset()
      assertion
    }
  }
}

class AssertableCommandBus extends CommandBus {
  private var commands = List.empty[SentCommand]
  override def send(aggregateId: String, command: Any, metadata: CommandMetadata): Task[Unit] = Task.now {
    commands = commands.appended(SentCommand(aggregateId: String, command: Any, metadata: CommandMetadata))
  }

  def reset(): Unit = {
    commands = List.empty
  }

  def sentCommands: Seq[SentCommand] = commands
}

object AssertableCommandBus {
  case class SentCommand(aggregateId: String, command: Any, metadata: CommandMetadata)
}
