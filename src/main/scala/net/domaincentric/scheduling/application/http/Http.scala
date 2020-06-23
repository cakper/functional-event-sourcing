package net.domaincentric.scheduling.application.http
import java.time.LocalDate

import io.circe.generic.auto._
import io.circe.syntax._
import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing.{ AggregateStore, CausationId, CorrelationId, EventMetadata }
import net.domaincentric.scheduling.domain.aggregate.doctorday._
import net.domaincentric.scheduling.domain.readmodel.avialbleslots.Repository
import net.domaincentric.scheduling.domain.service.UuidGenerator
import net.domaincentric.scheduling.infrastructure.circe.Implicits._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import scala.util.Try

object LocalDateVar {
  def unapply(str: String): Option[LocalDate] = {
    if (!str.isEmpty)
      Try(LocalDate.parse(str)).toOption
    else
      None
  }
}

object Http {
  val dsl: Http4sDsl[Task] = Http4sDsl[Task]
  import dsl._

  implicit def decodeScheduleDay: EntityDecoder[Task, ScheduleDay] = jsonOf

  def service(repository: Repository, aggregateStore: AggregateStore)(
      implicit uuidGenerator: UuidGenerator
  ): HttpRoutes[Task] = HttpRoutes.of[Task] {
    case GET -> Root / "slots" / LocalDateVar(date) / "available" =>
      Ok(repository.getAvailableSlotsOn(date).map(_.asJson))
    case req @ POST -> Root / "doctor" / "schedule" =>
      for {
        command <- req.as[ScheduleDay]
        aggregate <- Task.now(
          aggregateStore.create(DoctorDayId(command.doctorId, command.date), new CommandHandler)
        )
        resp <- aggregate.handle(command) match {
          case Left(error) => BadRequest(error.asJson)
          case Right(handled) =>
            aggregateStore
              .commit(handled, EventMetadata(CorrelationId.create, CausationId.create))
              .flatMap(_ => Created())
        }
      } yield resp
  }
}
