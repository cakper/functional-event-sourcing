package net.domaincentric.scheduling.application

import com.eventstore.dbclient.{ StreamsClient, Timeouts, UserCredentials }
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import javax.net.ssl.SSLException
import monix.eval.Task
import monix.reactive.Consumer
import net.domaincentric.scheduling.application.eventsourcing.{ PersistentSubscription, SubscriptionId }
import net.domaincentric.scheduling.application.http.Http
import net.domaincentric.scheduling.application.messagehandlers.{ AsyncCommandHandler, AvailableSlotsProjector, OverbookingProcessManager }
import net.domaincentric.scheduling.domain.service.{ RandomUuidGenerator, UuidGenerator }
import net.domaincentric.scheduling.infrastructure.eventstoredb._
import net.domaincentric.scheduling.infrastructure.mongodb.{ AvailableSlotsRepository, BookedSlotsRepository }
import org.http4s.HttpRoutes
import org.mongodb.scala.MongoClient

class ServiceContainer {
  private def getClientSslContext =
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
  val availableSlotsRepository              = new AvailableSlotsRepository(database)
  val availableSlotsProjector               = new AvailableSlotsProjector(availableSlotsRepository)
  val snapshotStore                         = new SnapshotStore(new EventStore(streamsClient, new StateSerde))
  val eventStore                            = new EventStore(streamsClient, new EventSerde)
  val aggregateStore                        = new AggregateStore(eventStore, snapshotStore)
  implicit val uuidGenerator: UuidGenerator = RandomUuidGenerator

  val httpService: HttpRoutes[Task] = new Http(availableSlotsRepository, aggregateStore).service

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
    new OverbookingProcessManager(new BookedSlotsRepository(database), commandBus, 1)
  val overbookingProcessManagerSubscription: Task[Unit] = PersistentSubscription(
    SubscriptionId("overbooking-process"),
    "$ce-doctorday",
    eventStore,
    checkpointStore,
  ).consumeWith(Consumer.foreachTask(overbookingProcessManager.handle))
}
