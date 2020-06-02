package net.domaincentric.scheduling

import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}

object Application extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] = ???
}
