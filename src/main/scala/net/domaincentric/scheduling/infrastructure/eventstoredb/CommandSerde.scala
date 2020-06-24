package net.domaincentric.scheduling.infrastructure.eventstoredb

import com.eventstore.dbclient.{ ProposedEvent, ResolvedEvent }
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import net.domaincentric.scheduling.application.eventsourcing.{ CommandMetadata, MessageEnvelope }
import net.domaincentric.scheduling.domain.writemodel.doctorday.CancelSlotBooking

import scala.util.Try

class CommandSerde extends Serde[CommandMetadata] {
  private val prefix = "doctorday:command"
  override def serialize(data: Any, metadata: CommandMetadata): Try[ProposedEvent] = Try {
    data match {
      case c: CancelSlotBooking => toProposedEvent(s"$prefix-cancel-slot-booking", c.asJson, metadata.asJson)
    }
  }

  override def deserialize(resolvedEvent: ResolvedEvent): Try[MessageEnvelope[CommandMetadata]] = Try {
    val event = (resolvedEvent.getEvent.getEventType match {
      case s"$prefix-cancel-slot-booking" => decode[CancelSlotBooking] _
    })(new String(resolvedEvent.getEvent.getEventData)).toOption.get

    val metadata = decode[CommandMetadata](new String(resolvedEvent.getEvent.getUserMetadata)).toOption.get
    toEnvelope(event, metadata, resolvedEvent)
  }
}
