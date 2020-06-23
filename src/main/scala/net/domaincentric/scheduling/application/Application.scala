package net.domaincentric.scheduling.application

import cats.effect.ExitCode
import com.eventstore.dbclient._
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import javax.net.ssl.SSLException
import monix.eval.{ Task, TaskApp }
import monix.reactive.Consumer
import net.domaincentric.scheduling.application.eventhandlers.AvailableSlotsProjector
import net.domaincentric.scheduling.application.http.Http
import net.domaincentric.scheduling.domain.service.{ RandomUuidGenerator, UuidGenerator }
import net.domaincentric.scheduling.infrastructure.eventstoredb.{ AggregateStore, EventSerde, EventStore }
import net.domaincentric.scheduling.infrastructure.mongodb.MongodbAvailableSlotsRepository
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.mongodb.scala.MongoClient

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

    val mongoDbClient                         = MongoClient("mongodb://localhost")
    val availableSlotsRepository              = new MongodbAvailableSlotsRepository(mongoDbClient.getDatabase("projections"))
    val availableSlotsProjector               = new AvailableSlotsProjector(availableSlotsRepository)
    val aggregateStore                        = new AggregateStore(eventStore)
    implicit val uuidGenerator: UuidGenerator = RandomUuidGenerator

    val httpServer: Task[Nothing] =
      BlazeServerBuilder[Task](scheduler)
        .bindHttp(8080, "localhost")
        .withHttpApp(Router("/api" -> Http.service(availableSlotsRepository, aggregateStore)).orNotFound)
        .resource
        .use(_ => Task.never)

    val projectionConsumer: Task[Unit] = eventStore
      .subscribeToAll()
      .consumeWith(Consumer.foreachTask { envelope =>
        availableSlotsProjector
          .handle(envelope.data, envelope.metadata, envelope.eventId, envelope.version, envelope.occurredAt)
      })

    Task
      .parZip2(httpServer, projectionConsumer)
      .guarantee {
        Task.parZip2(Task.eval(streamsClient.shutdown()), Task.eval(mongoDbClient.close())).map(_ => ())
      }
      .map(_ => ExitCode.Success)
  }
}
