package net.domaincentric.scheduling.application

import cats.effect.ExitCode
import monix.eval.{ Task, TaskApp }

object Application extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] = ???
}
