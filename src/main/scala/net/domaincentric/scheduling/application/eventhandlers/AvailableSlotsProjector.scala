package net.domaincentric.scheduling.application.eventhandlers

import java.time.Instant
import java.util.UUID

import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing.{ MessageHandler, EventMetadata, Version }
import net.domaincentric.scheduling.domain.aggregate.doctorday.{ SlotBooked, SlotBookingCancelled, SlotScheduled }
import net.domaincentric.scheduling.domain.readmodel.avialbleslots.{ AvailableSlot, Repository }

class AvailableSlotsProjector(val repository: Repository) extends MessageHandler[EventMetadata] {
  override def handle(
      event: Any,
      metadata: EventMetadata,
      eventId: UUID,
      position: Version,
      occurredAt: Instant,
      streamPosition: Option[Version]
  ): Task[Unit] = event match {
    case e: SlotScheduled =>
      repository
        .addSlot(
          AvailableSlot(e.dayId, e.slotId, e.startTime.toLocalDate, e.startTime.toLocalTime, e.duration.toString())
        )
        .map(
          _ =>
            println(
              s"Added slot ${e.slotId} with start time ${e.startTime} and duration ${e.duration} to available slots"
          )
        )
    case e: SlotBooked           => repository.hideSlot(e.slotId).map(_ => println("slot hidden"))
    case e: SlotBookingCancelled => repository.showSlot(e.slotId).map(_ => println("slot shown"))
    case _                       => Task.unit
  }
}
