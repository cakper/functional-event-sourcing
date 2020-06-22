package net.domaincentric.scheduling.infrastructure.circe

import java.util.concurrent.TimeUnit

import io.circe.{ Decoder, DecodingFailure, Encoder, HCursor, Json, JsonObject }

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
}
