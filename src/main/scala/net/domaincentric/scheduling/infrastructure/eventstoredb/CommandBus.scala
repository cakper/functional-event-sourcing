package net.domaincentric.scheduling.infrastructure.eventstoredb

import com.eventstore.dbclient.StreamsClient
import monix.eval.Task
import monix.reactive.Observable
import net.domaincentric.scheduling.application.eventsourcing
import net.domaincentric.scheduling.application.eventsourcing.CommandBus.CommandEnvelope
import net.domaincentric.scheduling.application.eventsourcing.{ CommandMetadata, MessageEnvelope, PersistentSubscription, SubscriptionId }

class CommandBus(client: StreamsClient, streamName: String) extends eventsourcing.CommandBus {
  private val commandStore = new EventStore[CommandMetadata](client, new CommandSerde)

  def send(command: Any, metadata: CommandMetadata): Task[Unit] =
    commandStore.appendToStream(streamName, Seq(command), metadata).map(_ => ())

  def subscribe(): Observable[CommandEnvelope] = {
    PersistentSubscription(
      SubscriptionId(streamName),
      streamName,
      commandStore,
      new CheckpointStore(new EventStore(client, new CheckpointSerde)),
    ).map { envelope =>
      CommandEnvelope(envelope.data, envelope.metadata)
    }
  }
}
