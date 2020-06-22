package net.domaincentric.scheduling.infrastructure.eventstoredb

import monix.eval.Task
import monix.reactive.Observable
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.CommandBus.CommandEnvelope
import net.domaincentric.scheduling.application.eventsourcing.{ CommandMetadata, Envelope }

class CommandBus(eventStore: EventStore[CommandMetadata], streamName: String = "commands")
    extends eventsourcing.CommandBus {
  def send(command: Any, metadata: CommandMetadata): Task[Unit] =
    eventStore
      .appendToStream(
        streamName,
        Seq(command),
        metadata
      )
      .map(_ => ())
  def subscribe(): Observable[CommandEnvelope] =
    eventStore
      .readFromStream(streamName)
      .map {
        case Envelope(command, metadata, _, _, _) =>
          CommandEnvelope(
            command,
            CommandMetadata(metadata.correlationId, metadata.causationId, metadata.aggregateId)
          )
      }
//      .doOnNextAck()
}

object CommandBus {
  case class AggregateIdAndCommand(aggregateId: String, command: Any)
}
