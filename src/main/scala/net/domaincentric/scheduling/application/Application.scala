package net.domaincentric.scheduling.application

import cats.effect.ExitCode
import com.eventstore.dbclient._
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import javax.net.ssl.SSLException
import monix.eval.{ Task, TaskApp }

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

    val task1 = Task.deferFuture(
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

    val task2 = Task.deferFuture(
      streamsClient
        .subscribeToStream(
          "doctorday-c0623d12",
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
      _ <- task1.delayResult(5.seconds)
//      _ <- task2.delayResult(5.seconds)
    } yield ExitCode.Success
  }
}
