package net.domaincentric.scheduling.readmodel.availableslots

import java.time.Instant
import java.util.UUID

import monix.eval.Task
import net.domaincentric.scheduling.application.eventsourcing.{ EventHandler, EventMetadata, Version }
import net.domaincentric.scheduling.domain.doctorday.{ SlotBooked, SlotBookingCancelled, SlotScheduled }
import net.domaincentric.scheduling.eventsourcing.Event

class Projector(val repository: Repository) extends EventHandler {
  override def handle[E <: Event](
      event: E,
      metadata: EventMetadata,
      eventId: UUID,
      position: Version,
      occurredAt: Instant
  ): Task[Unit] = event match {
    case e: SlotScheduled =>
      repository.addSlot(
        AvailableSlot(e.dayId, e.slotId, e.startTime.toLocalDate, e.startTime.toLocalTime, e.duration.toString())
      )
    case e: SlotBooked           => repository.hideSlot(e.slotId)
    case e: SlotBookingCancelled => repository.showSlot(e.slotId)
  }
}
