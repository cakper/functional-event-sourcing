package net.domaincentric.scheduling.application

import cats.effect.ExitCode
import com.eventstore.dbclient._
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import javax.net.ssl.SSLException
import monix.eval.{ Task, TaskApp }
import monix.reactive.Consumer
import net.domaincentric.scheduling.application.eventhandlers.AvailableSlotsProjector
import net.domaincentric.scheduling.infrastructure.eventstoredb.{ EventSerde, EventStore }
import net.domaincentric.scheduling.infrastructure.mongodb.MongodbAvailableSlotsRepository
import org.mongodb.scala.{ MongoClient, MongoDatabase }

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

    val mongoDbClient            = MongoClient("mongodb://localhost")
    val availableSlotsRepository = new MongodbAvailableSlotsRepository(mongoDbClient.getDatabase("projections"))
    val availableSlotsProjector  = new AvailableSlotsProjector(availableSlotsRepository)

    (for {
      _ <- eventStore
        .subscribeToAll()
        .consumeWith(Consumer.foreachTask { envelope =>
          availableSlotsProjector
            .handle(envelope.event, envelope.metadata, envelope.eventId, envelope.version, envelope.occurredAt)
        })

    } yield ExitCode.Success)
      .guarantee {
        Task.parZip2(Task.eval(streamsClient.shutdown()), Task.eval(mongoDbClient.close())).map(_ => ())
      }
  }
}
