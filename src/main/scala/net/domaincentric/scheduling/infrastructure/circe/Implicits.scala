package net.domaincentric.scheduling.infrastructure.circe

import java.util.concurrent.TimeUnit

import io.circe.generic.extras.semiauto.{ deriveUnwrappedDecoder, deriveUnwrappedEncoder }
import io.circe.{ Decoder, DecodingFailure, Encoder, HCursor, Json, JsonObject }
import net.domaincentric.scheduling.application.eventsourcing.{ CausationId, CorrelationId, Version }
import net.domaincentric.scheduling.domain.aggregate.doctorday.{ DayId, DoctorId, PatientId, SlotId }

import scala.concurrent.duration.FiniteDuration

object Implicits {
  implicit final val finiteDurationDecoder: Decoder[FiniteDuration] =
    (c: HCursor) =>
      for {
        length     <- c.downField("length").as[Long]
        unitString <- c.downField("unit").as[String]
        unit <- try {
          Right(TimeUnit.valueOf(unitString))
        } catch {
          case _: IllegalArgumentException => Left(DecodingFailure("FiniteDuration", c.history))
        }
      } yield FiniteDuration(length, unit)

  implicit final val finiteDurationEncoder: Encoder[FiniteDuration] = (a: FiniteDuration) =>
    Json.fromJsonObject(JsonObject("length" -> Json.fromLong(a.length), "unit" -> Json.fromString(a.unit.name)))

  implicit val slotIdEncoder: Encoder[SlotId]               = deriveUnwrappedEncoder
  implicit val slotIdDecoder: Decoder[SlotId]               = deriveUnwrappedDecoder
  implicit val dayIdEncoder: Encoder[DayId]                 = deriveUnwrappedEncoder
  implicit val dayIdDecoder: Decoder[DayId]                 = deriveUnwrappedDecoder
  implicit val correlationIdEncoder: Encoder[CorrelationId] = deriveUnwrappedEncoder
  implicit val correlationIdDecoder: Decoder[CorrelationId] = deriveUnwrappedDecoder
  implicit val causationIdEncoder: Encoder[CausationId]     = deriveUnwrappedEncoder
  implicit val causationIdDecoder: Decoder[CausationId]     = deriveUnwrappedDecoder
  implicit val doctorIdEncoder: Encoder[DoctorId]           = deriveUnwrappedEncoder
  implicit val doctorIdDecoder: Decoder[DoctorId]           = deriveUnwrappedDecoder
  implicit val versionEncoder: Encoder[Version]             = deriveUnwrappedEncoder
  implicit val versionDecoder: Decoder[Version]             = deriveUnwrappedDecoder
  implicit val patientIdEncoder: Encoder[PatientId]         = deriveUnwrappedEncoder
  implicit val patientIdDecoder: Decoder[PatientId]         = deriveUnwrappedDecoder
}
