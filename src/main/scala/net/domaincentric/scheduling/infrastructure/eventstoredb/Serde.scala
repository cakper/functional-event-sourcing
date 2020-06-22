package net.domaincentric.scheduling.infrastructure.eventstoredb

import java.util.UUID
import java.util.concurrent.TimeUnit

import com.eventstore.dbclient.{ ProposedEvent, ResolvedEvent }
import io.circe._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.{ Envelope, EventMetadata }
import net.domaincentric.scheduling.domain.aggregate.doctorday._

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

trait Serde[M] {
  def serialize(data: Any, metadata: M): Try[ProposedEvent]
  def deserialize(resolvedEvent: ResolvedEvent): Try[Envelope[M]]
}
