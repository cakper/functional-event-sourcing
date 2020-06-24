package net.domaincentric.scheduling.infrastructure.eventstoredb

import com.eventstore.dbclient.{ ProposedEvent, ResolvedEvent }
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import net.domaincentric.scheduling.application.eventsourcing.{ Checkpoint, MessageEnvelope }

import scala.util.Try

class CheckpointSerde extends Serde[Unit] {
  override def serialize(data: Any, metadata: Unit): Try[ProposedEvent] = Try {
    data match {
      case c: Checkpoint => toProposedEvent("checkpoint", c.asJson, ().asJson)
    }
  }

  override def deserialize(resolvedEvent: ResolvedEvent): Try[MessageEnvelope[Unit]] = Try {
    val event = decode[Checkpoint](new String(resolvedEvent.getEvent.getEventData)).toOption.get
    toEnvelope(event, (), resolvedEvent)
  }
}
