package net.domaincentric.scheduling.application.eventsourcing

import monix.eval.Task
import monix.reactive.Observable

object PersistentSubscription {
  def apply[M](
      subscriptionId: SubscriptionId,
      streamId: String,
      eventStore: EventStore[M],
      checkpointStore: CheckpointStore
  ): Observable[MessageEnvelope[M]] = {
    val subscriptionTask = for {
      maybeStartCheckpoint <- checkpointStore.read(subscriptionId)
      subscription <- Task.now(
        eventStore
          .subscribeToStream(streamId, maybeStartCheckpoint)
          .doOnSubscribe(
            Task.eval(
              println(s"Starting '$subscriptionId' subscription from: ${maybeStartCheckpoint.getOrElse("beginning")}")
            )
          )
          .doOnSubscriptionCancel(
            Task.eval(
              println(s"Starting '$subscriptionId' was cancelled}")
            )
          )
          .doOnNextAck {
            case (envelope, _) =>
              checkpointStore
                .update(subscriptionId, Checkpoint(envelope.streamPosition.getOrElse(envelope.version).value))
          }
      )
    } yield subscription

    Observable.fromTask(subscriptionTask).concat
  }
}
