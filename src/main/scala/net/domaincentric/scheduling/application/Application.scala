package net.domaincentric.scheduling.application

import java.time.LocalDate
import java.util.UUID

import cats.effect.ExitCode
import com.eventstore.dbclient._
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import javax.net.ssl.SSLException
import monix.eval.{ Task, TaskApp }
import net.domaincentric.scheduling.application.eventsourcing.EventMetadata
import net.domaincentric.scheduling.domain.aggregate.doctorday.DayScheduled
import net.domaincentric.scheduling.infrastructure.eventstoredb.{ EventSerde, EventStore }

import scala.compat.java8.FutureConverters._
import scala.concurrent.duration._

object Application extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] = {
    def getClientSslContext =
      try GrpcSslContexts.forClient.trustManager(InsecureTrustManagerFactory.INSTANCE).build
      catch {
        case _: SSLException => null
      }

    val streamsClient: StreamsClient = {
      val creds = new UserCredentials("admin", "changeit")
      new StreamsClient("localhost", 2113, creds, Timeouts.DEFAULT, getClientSslContext)
    }

    val eventStore = new EventStore(streamsClient, new EventSerde)

    val streamId = "doctorday-" + UUID.randomUUID()
    val writeEvents =
      eventStore.createNewStream(
        streamId,
        Seq(DayScheduled(UUID.randomUUID(), "John Doe", LocalDate.now())),
        EventMetadata("123", "abc")
      )

    val allSubscription = Task.deferFuture(
      streamsClient
        .subscribeToAll(
          Position.START,
          true,
          new SubscriptionListener {
            override def onError(subscription: Subscription, throwable: Throwable): Unit = println(throwable)
            override def onEvent(subscription: Subscription, event: ResolvedEvent): Unit = println(event)
            override def onCancelled(subscription: Subscription): Unit                   = println("cancelled")
          }
        )
        .toScala
    )

    val streamSubscription = Task.deferFuture(
      streamsClient
        .subscribeToStream(
          streamId,
          StreamRevision.START,
          false,
          new SubscriptionListener {
            override def onError(subscription: Subscription, throwable: Throwable): Unit = println(throwable)
            override def onEvent(subscription: Subscription, event: ResolvedEvent): Unit = println(event.getEvent)
            override def onCancelled(subscription: Subscription): Unit                   = println("cancelled")
          }
        )
        .toScala
    )

    for {
      _ <- writeEvents.delayResult(2.seconds)
      _ <- allSubscription.delayResult(2.seconds)
      _ <- streamSubscription.delayResult(2.seconds)
    } yield ExitCode.Success
  }
}
