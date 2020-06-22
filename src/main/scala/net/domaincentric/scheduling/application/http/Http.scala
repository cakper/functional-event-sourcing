package net.domaincentric.scheduling.application.http
import monix.eval.Task
import org.http4s._
import org.http4s.dsl.Http4sDsl

object Http {
  val dsl: Http4sDsl[Task] = Http4sDsl[Task]
  import dsl._

  val service: HttpRoutes[Task] = HttpRoutes.of[Task] {
    case GET -> Root / "hello" / name =>
      Ok(s"Hello, $name.")
  }
}
