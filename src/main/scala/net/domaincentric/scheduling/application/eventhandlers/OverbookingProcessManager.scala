package net.domaincentric.scheduling.application.eventhandlers

import java.time.Instant
import java.util.UUID

import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing.{ CommandBus, CommandMetadata, EventHandler, EventMetadata, Version }
import net.domaincentric.scheduling.domain.aggregate.doctorday.{ CancelSlotBooking, SlotBooked, SlotBookingCancelled, SlotScheduled }
import net.domaincentric.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository
import net.domaincentric.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository.Slot
import net.domaincentric.scheduling.domain.service.UuidGenerator

class OverbookingProcessManager(repository: BookedSlotsRepository, commandBus: CommandBus, bookingLimitPerPatient: Int)(
    implicit uuidGenerator: UuidGenerator
) extends EventHandler {
  override def handle(
      event: Any,
      metadata: EventMetadata,
      eventId: UUID,
      position: Version,
      occurredAt: Instant
  ): Task[Unit] = event match {
    case SlotScheduled(slotId, dayId, startTime, _) => repository.addSlot(Slot(slotId, dayId, startTime.getMonth))
    case SlotBooked(slotId, patientId) =>
      for {
        _     <- repository.markSlotAsBooked(slotId, patientId)
        slot  <- repository.findSlot(slotId)
        count <- repository.countByPatientAndMonth(patientId, slot.month)
        _ <- if (count <= bookingLimitPerPatient) Task.unit
        else
          commandBus.send(
            slot.dayId.toString,
            CancelSlotBooking(slotId, "Overbooking"),
            CommandMetadata(metadata.correlationId, uuidGenerator.next().toString)
          )
      } yield ()

    case SlotBookingCancelled(slotId, _) => repository.markSlotAsAvailable(slotId)
    case _                               => Task.raiseError(new RuntimeException("Handler not implemented"))
  }
}
