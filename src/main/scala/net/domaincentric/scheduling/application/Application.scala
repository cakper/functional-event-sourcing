package net.domaincentric.scheduling.application

import cats.effect.ExitCode
import monix.eval.{ Task, TaskApp }
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

object Application extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] = {
    val container: ServiceContainer = new ServiceContainer

    val httpServer: Task[Nothing] =
      BlazeServerBuilder[Task](scheduler)
        .bindHttp(8888, "localhost")
        .withHttpApp(Router("/api" -> container.httpService).orNotFound)
        .resource
        .use(_ => Task.never)

    Task
      .parZip4(
        httpServer,
        container.availableSlotsProjectionSubscription,
        container.commandHandlerSubscription,
        container.overbookingProcessManagerSubscription
      )
      .guarantee {
        Task
          .parZip2(Task.eval(container.streamsClient.shutdown()), Task.eval(container.mongoDbClient.close()))
          .map(_ => ())
      }
      .map(_ => ExitCode.Success)
  }
}
