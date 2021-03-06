package net.domaincentric.scheduling.application.messagehandlers

import java.time.Instant
import java.util.UUID

import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing.{ AggregateId, CausationId, CommandBus, CommandMetadata, CorrelationId, EventMetadata, MessageHandler, Version }
import net.domaincentric.scheduling.domain.writemodel.doctorday.{ CancelSlotBooking, DoctorDayId, SlotBooked, SlotBookingCancelled, SlotScheduled }
import net.domaincentric.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository
import net.domaincentric.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository.Slot
import net.domaincentric.scheduling.domain.service.UuidGenerator
import net.domaincentric.scheduling.infrastructure.mongodb.UnableToFindSlot

class OverbookingProcessManager(repository: BookedSlotsRepository, commandBus: CommandBus, bookingLimitPerPatient: Int)(
    implicit uuidGenerator: UuidGenerator
) extends MessageHandler[EventMetadata] {
  override def handle(
      event: Any,
      metadata: EventMetadata,
      eventId: UUID,
      position: Version,
      occurredAt: Instant,
      streamPosition: Option[Version]
  ): Task[Unit] = event match {
    case SlotScheduled(slotId, dayId, startTime, _) => repository.addSlot(Slot(slotId, dayId, startTime.getMonth))
    case SlotBooked(slotId, patientId) =>
      repository.findAllSlotIdsFor(patientId).flatMap {
        case slotIds if slotIds.contains(slotId) => Task.unit
        case _ =>
          (for {
            _     <- repository.markSlotAsBooked(slotId, patientId)
            slot  <- repository.getSlot(slotId)
            count <- repository.countByPatientAndMonth(patientId, slot.month)
            _ <- if (count <= bookingLimitPerPatient) Task.unit
            else
              commandBus.send(
                CancelSlotBooking(slotId, "Overbooking"),
                CommandMetadata(metadata.correlationId, CausationId.create, DoctorDayId(slot.dayId))
              )
          } yield ()).onErrorRecover {
            case UnableToFindSlot(_) => ()
          }
      }

    case SlotBookingCancelled(slotId, _) => repository.markSlotAsAvailable(slotId)
    case _                               => Task.unit
  }
}
