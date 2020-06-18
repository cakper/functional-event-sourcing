package net.domaincentric.scheduling.infrastructure.eventstoredb

import java.util.concurrent.CompletableFuture

import com.eventstore.dbclient.{ ResolvedEvent, Subscription, SubscriptionListener }
import monix.catnap.ConcurrentQueue
import monix.eval.Task
import monix.reactive.Observable

import scala.compat.java8.FutureConverters._

object SubscriptionObservable {
  def apply(
      factory: SubscriptionListener => CompletableFuture[Subscription],
      options: SubscriptionOptions = SubscriptionOptions.default
  ): Observable[ResolvedEvent] = {
    Observable
      .fromTask(
        ConcurrentQueue
          .bounded[Task, Either[Option[Throwable], ResolvedEvent]](options.bufferCapacity)
          .flatMap { queue =>
            for {
              subscription <- Task
                .deferFutureAction(implicit s => factory(new QueueBackedSubscriptionListener(queue)).toScala)
                .timeout(options.connectionTimeout)
            } yield
              Observable
                .repeatEvalF(queue.poll)
                .takeWhileInclusive(_.isRight)
                .flatMap {
                  case Right(value)          => Observable.now(value)
                  case Left(None)            => Observable.empty
                  case Left(Some(throwable)) => Observable.raiseError(throwable)
                }
                .guarantee(Task.eval(subscription.stop()))
                .doOnSubscriptionCancel(Task.eval(println("Subscription was cancelled")))
          }
      )
      .concat
  }
}
