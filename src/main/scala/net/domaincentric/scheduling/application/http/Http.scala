package net.domaincentric.scheduling.application.http
import java.time.LocalDate

import io.circe.generic.auto._
import io.circe.syntax._
import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing._
import net.domaincentric.scheduling.domain.readmodel.avialbleslots.AvailableSlotsRepository
import net.domaincentric.scheduling.domain.service.UuidGenerator
import net.domaincentric.scheduling.domain.writemodel.doctorday._
import net.domaincentric.scheduling.infrastructure.circe.Implicits._
import net.domaincentric.scheduling.infrastructure.eventstoredb.StreamAlreadyExists
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.util.CaseInsensitiveString

import scala.util.Try

object LocalDateVar {
  def unapply(str: String): Option[LocalDate] = {
    if (!str.isEmpty)
      Try(LocalDate.parse(str)).toOption
    else
      None
  }
}

class Http(repository: AvailableSlotsRepository, aggregateStore: AggregateStore)(
    implicit uuidGenerator: UuidGenerator
) {
  val dsl: Http4sDsl[Task] = Http4sDsl[Task]
  import dsl._

  implicit def scheduleDayDecoder: EntityDecoder[Task, ScheduleDay]             = jsonOf
  implicit def bookSlotDecoder: EntityDecoder[Task, BookSlot]                   = jsonOf
  implicit def cancelSlotBookingDecoder: EntityDecoder[Task, CancelSlotBooking] = jsonOf

  val service: HttpRoutes[Task] = HttpRoutes.of[Task] {
    case GET -> Root / "slots" / LocalDateVar(date) / "available" =>
      Ok(repository.getAvailableSlotsOn(date).map(_.asJson))
    case req @ POST -> Root / "doctor" / "schedule" =>
      for {
        command <- req.as[ScheduleDay]
        aggregate = Aggregate(DoctorDayId(command.doctorId, command.date), new DoctorDayRules)
        resp <- aggregate.handle(command) match {
          case Left(error) => BadRequest(error.asJson)
          case Right(handled) =>
            aggregateStore
              .commit(handled, metadata(req))
              .flatMap(_ => Created())
              .onErrorHandleWith {
                case StreamAlreadyExists => BadRequest(Map("reason" -> "Day was already planned").asJson)
              }
        }
      } yield resp

    case req @ POST -> Root / "slots" / DoctorDayId(id) / "book" =>
      handleCommand[BookSlot](req, id)
    case req @ POST -> Root / "slots" / DoctorDayId(id) / "cancel" =>
      handleCommand[CancelSlotBooking](req, id)
  }

  private def handleCommand[C <: Command](
      request: Request[Task],
      doctorDayId: AggregateId
  )(implicit entityDecoder: EntityDecoder[Task, C]) =
    for {
      command   <- request.as[C]
      aggregate <- aggregateStore.reconsititute(Aggregate(doctorDayId, new DoctorDayRules))
      result <- aggregate.handle(command) match {
        case Right(handled) => aggregateStore.commit(handled, metadata(request)).flatMap(_ => Ok())
        case Left(error)    => BadRequest(error.asJson)
      }
    } yield result

  private def metadata(request: Request[Task]): EventMetadata = {
    EventMetadata(
      request.headers
        .get(CaseInsensitiveString("X-CorrelationId"))
        .map(h => CorrelationId.apply(h.value))
        .getOrElse(CorrelationId.create),
      request.headers
        .get(CaseInsensitiveString("X-CausationId"))
        .map(h => CausationId.apply(h.value))
        .getOrElse(CausationId.create)
    )
  }
}
