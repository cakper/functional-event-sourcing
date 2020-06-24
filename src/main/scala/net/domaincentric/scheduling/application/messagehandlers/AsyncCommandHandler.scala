package net.domaincentric.scheduling.application.messagehandlers

import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing.CommandBus.CommandEnvelope
import net.domaincentric.scheduling.application.eventsourcing.{ Aggregate, AggregateStore, EventMetadata }
import net.domaincentric.scheduling.domain.writemodel.doctorday.{ CancelSlotBooking, DoctorDayRules }
import net.domaincentric.scheduling.domain.service.UuidGenerator

class AsyncCommandHandler(aggregateStore: AggregateStore)(implicit uuidGenerator: UuidGenerator) {
  def handle(commandEnvelope: CommandEnvelope): Task[Unit] = commandEnvelope.command match {
    case command: CancelSlotBooking =>
      for {
        aggregate <- aggregateStore.reconsititute(Aggregate(commandEnvelope.metadata.aggregateId, new DoctorDayRules))
        _ <- aggregate.handle(command) match {
          case Left(error) => Task.eval(println(s"Unable to handle command due to: $error"))
          case Right(handled) =>
            aggregateStore.commit(
              handled,
              EventMetadata(commandEnvelope.metadata.correlationId, commandEnvelope.metadata.causationId)
            )
        }
      } yield ()

    case _ => Task.unit
  }
}
