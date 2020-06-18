package net.domaincentric.scheduling.infrastructure.eventstoredb

import com.eventstore.dbclient.{ ResolvedEvent, Subscription, SubscriptionListener }
import monix.catnap.ConcurrentQueue
import monix.eval.Task
import monix.execution.Scheduler

class QueueBackedSubscriptionListener(queue: ConcurrentQueue[Task, Either[Option[Throwable], ResolvedEvent]])(
    implicit scheduler: Scheduler
) extends SubscriptionListener {
  override def onEvent(subscription: Subscription, event: ResolvedEvent): Unit =
    queue.offer(Right(event)).runSyncUnsafe()

  override def onError(subscription: Subscription, throwable: Throwable): Unit =
    queue.offer(Left(Some(throwable))).runSyncUnsafe()

  override def onCancelled(subscription: Subscription): Unit = queue.offer(Left(None)).runSyncUnsafe()
}
