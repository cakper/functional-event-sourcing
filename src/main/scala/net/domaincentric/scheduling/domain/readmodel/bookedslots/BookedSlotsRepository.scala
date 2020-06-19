package net.domaincentric.scheduling.domain.readmodel.bookedslots

import java.time.Month

import monix.eval.Task
import net.domaincentric.scheduling.domain.aggregate.doctorday.{ DayId, SlotId }
import net.domaincentric.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository.Slot

trait BookedSlotsRepository {
  def addSlot(slot: Slot): Task[Unit]
  def markSlotAsBooked(slotId: SlotId, patientId: String): Task[Unit]
  def markSlotAsAvailable(slotId: SlotId): Task[Unit]
  def countByPatientAndMonth(patient: String, month: Month): Task[Int]
  def findSlot(slotId: SlotId): Task[Slot]
}

object BookedSlotsRepository {
  case class Slot(slotId: SlotId, dayId: DayId, month: Month)
}
