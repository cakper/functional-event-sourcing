package net.domaincentric.scheduling.infrastructure.eventstoredb

import java.util.UUID

import com.eventstore.dbclient.{ ProposedEvent, ResolvedEvent }
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.{ MessageEnvelope, SnapshotMetadata, Version }
import net.domaincentric.scheduling.domain.aggregate.doctorday.State
import net.domaincentric.scheduling.infrastructure.circe.Implicits._

import scala.util.Try

class StateSerde extends Serde[SnapshotMetadata] {
  private val prefix = "state"
  override def serialize(data: Any, metadata: SnapshotMetadata): Try[ProposedEvent] = Try {
    data match {
      case s: State => toProposedEvent(s"$prefix-doctorday", s.asJson, metadata)
    }
  }
  private def toProposedEvent(`type`: String, data: Json, metadata: SnapshotMetadata) = {
    new ProposedEvent(
      UUID.randomUUID(),
      `type`,
      "application/json",
      data.noSpaces.getBytes,
      metadata.asJson.noSpaces.getBytes
    )
  }

  override def deserialize(resolvedEvent: ResolvedEvent): Try[MessageEnvelope[SnapshotMetadata]] = Try {
    val raw = resolvedEvent.getEvent
    val event = (raw.getEventType match {
      case s"$prefix-doctorday" => decode[State] _
    })(new String(raw.getEventData)).toOption.get

    val metadata = decode[SnapshotMetadata](new String(raw.getUserMetadata)).toOption.get
    eventsourcing.MessageEnvelope(
      event,
      metadata,
      raw.getEventId,
      raw.getStreamRevision.getValueUnsigned,
      raw.getCreated,
      Option(resolvedEvent.getLink).map(_.getStreamRevision.getValueUnsigned)
    )
  }
}
