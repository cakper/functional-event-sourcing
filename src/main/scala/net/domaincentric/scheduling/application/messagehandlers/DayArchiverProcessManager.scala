package net.domaincentric.scheduling.application.messagehandlers

import java.time.Instant
import java.util.UUID

import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing._
import net.domaincentric.scheduling.domain.readmodel.archivableday.ArchivableDaysRepository
import net.domaincentric.scheduling.domain.service.UuidGenerator
import net.domaincentric.scheduling.domain.writemodel.calendar.CalendarDayStarted
import net.domaincentric.scheduling.domain.writemodel.doctorday.{ Archive, DayScheduleArchived, DayScheduled, DoctorDayId }

class DayArchiverProcessManager(
    coldStorage: ColdStorage,
    eventStore: EventStore[EventMetadata],
    repository: ArchivableDaysRepository,
    commandBus: CommandBus
)(implicit uuidGenerator: UuidGenerator)
    extends MessageHandler[EventMetadata] {
  override def handle(
      message: Any,
      metadata: EventMetadata,
      messageId: UUID,
      position: Version,
      occurredAt: Instant,
      streamPosition: Option[Version]
  ): Task[Unit] = message match {
    case DayScheduled(dayId, _, date) => repository.add(date, dayId)
    case CalendarDayStarted(date) =>
      repository
        .find(date.minusDays(180))
        .flatMap { days =>
          Task.traverse(days) { dayId =>
            commandBus.send(Archive(), CommandMetadata(metadata.correlationId, CausationId.create, DoctorDayId(dayId)))
          }
        }
        .map(_ => ())
    case DayScheduleArchived(dayId) =>
      for {
        events <- eventStore.readFromStream(DoctorDayId(dayId).toString).toListL
        _ <- events.reverse.headOption match {
          case Some(lastEvent) =>
            for {
              _ <- coldStorage.saveAll(DoctorDayId(dayId), events)
              _ <- eventStore.truncateStream(DoctorDayId(dayId).toString, lastEvent.version)
            } yield ()
          case None => Task.unit
        }
      } yield ()
    case _ => Task.unit
  }
}
