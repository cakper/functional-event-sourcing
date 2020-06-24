package net.domaincentric.scheduling.application

import cats.effect.ExitCode
import com.eventstore.dbclient._
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import javax.net.ssl.SSLException
import monix.eval.{ Task, TaskApp }
import monix.reactive.Consumer
import net.domaincentric.scheduling.application.eventhandlers.{ AsyncCommandHandler, AvailableSlotsProjector, OverbookingProcessManager }
import net.domaincentric.scheduling.application.eventsourcing.{ PersistentSubscription, SubscriptionId }
import net.domaincentric.scheduling.application.http.Http
import net.domaincentric.scheduling.domain.service.{ RandomUuidGenerator, UuidGenerator }
import net.domaincentric.scheduling.infrastructure.eventstoredb._
import net.domaincentric.scheduling.infrastructure.mongodb.{ MongoDbBookedSlotsRepository, MongodbAvailableSlotsRepository }
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

    val mongoDbClient                         = MongoClient("mongodb://localhost")
    val database                              = mongoDbClient.getDatabase("projections")
    val availableSlotsRepository              = new MongodbAvailableSlotsRepository(database)
    val availableSlotsProjector               = new AvailableSlotsProjector(availableSlotsRepository)
    val snapshotStore                         = new SnapshotStore(new EventStore(streamsClient, new StateSerde))
    val eventStore                            = new EventStore(streamsClient, new EventSerde)
    val aggregateStore                        = new AggregateStore(eventStore, snapshotStore)
    implicit val uuidGenerator: UuidGenerator = RandomUuidGenerator

    val httpServer: Task[Nothing] =
      BlazeServerBuilder[Task](scheduler)
        .bindHttp(8080, "localhost")
        .withHttpApp(Router("/api" -> Http.service(availableSlotsRepository, aggregateStore)).orNotFound)
        .resource
        .use(_ => Task.never)

    val checkpointStore = new CheckpointStore(new EventStore(streamsClient, new CheckpointSerde))
    val availableSlotsProjectionSubscription: Task[Unit] = PersistentSubscription(
      SubscriptionId("available-slots"),
      "$ce-doctorday",
      eventStore,
      checkpointStore,
    ).consumeWith(Consumer.foreachTask(availableSlotsProjector.handle))

    val commandBus                 = new CommandBus(streamsClient, "doctorday:commands")
    val commandHandler             = new AsyncCommandHandler(aggregateStore)
    val commandHandlerSubscription = commandBus.subscribe().consumeWith(Consumer.foreachTask(commandHandler.handle))

    val overbookingProcessManager =
      new OverbookingProcessManager(new MongoDbBookedSlotsRepository(database), commandBus, 1)
    val overbookingProcessManagerSubscription: Task[Unit] = PersistentSubscription(
      SubscriptionId("overbooking-process"),
      "$ce-doctorday",
      eventStore,
      checkpointStore,
    ).consumeWith(Consumer.foreachTask(overbookingProcessManager.handle))

    Task
      .parZip4(
        httpServer,
        availableSlotsProjectionSubscription,
        commandHandlerSubscription,
        overbookingProcessManagerSubscription
      )
      .guarantee {
        Task.parZip2(Task.eval(streamsClient.shutdown()), Task.eval(mongoDbClient.close())).map(_ => ())
      }
      .map(_ => ExitCode.Success)
  }
}
