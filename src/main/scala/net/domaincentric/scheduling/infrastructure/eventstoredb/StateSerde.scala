package net.domaincentric.scheduling.infrastructure.eventstoredb

import com.eventstore.dbclient.{ ProposedEvent, ResolvedEvent }
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import net.domaincentric.scheduling.application.eventsourcing.{ MessageEnvelope, SnapshotMetadata }
import net.domaincentric.scheduling.domain.aggregate.doctorday.State
import net.domaincentric.scheduling.infrastructure.circe.Implicits._

import scala.util.Try

class StateSerde extends Serde[SnapshotMetadata] {
  private val prefix = "state"
  override def serialize(data: Any, metadata: SnapshotMetadata): Try[ProposedEvent] = Try {
    data match {
      case s: State => toProposedEvent(s"$prefix-doctorday", s.asJson, metadata.asJson)
    }
  }

  override def deserialize(resolvedEvent: ResolvedEvent): Try[MessageEnvelope[SnapshotMetadata]] = Try {
    val event = (resolvedEvent.getEvent.getEventType match {
      case s"$prefix-doctorday" => decode[State] _
    })(new String(resolvedEvent.getEvent.getEventData)).toOption.get

    val metadata = decode[SnapshotMetadata](new String(resolvedEvent.getEvent.getUserMetadata)).toOption.get

    toEnvelope(event, metadata, resolvedEvent)
  }
}
