package net.domaincentric.scheduling.infrastructure.eventstoredb

import com.eventstore.dbclient.{ ProposedEvent, ResolvedEvent }
import net.domaincentric.scheduling.application.eventsourcing.MessageEnvelope

import scala.util.Try

trait Serde[M] {
  def serialize(data: Any, metadata: M): Try[ProposedEvent]
  def deserialize(resolvedEvent: ResolvedEvent): Try[MessageEnvelope[M]]
}
