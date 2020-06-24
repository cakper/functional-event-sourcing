package net.domaincentric.scheduling.domain.readmodel.bookedslots

import java.time.Month

import monix.eval.Task
import net.domaincentric.scheduling.domain.aggregate.doctorday.{ DayId, PatientId, SlotId }
import net.domaincentric.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository.Slot

trait BookedSlotsRepository {
  def addSlot(slot: Slot): Task[Unit]
  def markSlotAsBooked(slotId: SlotId, patientId: PatientId): Task[Unit]
  def markSlotAsAvailable(slotId: SlotId): Task[Unit]
  def countByPatientAndMonth(patientId: PatientId, month: Month): Task[Int]
  def findSlot(slotId: SlotId): Task[Slot]
  def findAllSlotIdsFor(patientId: PatientId): Task[Seq[SlotId]]
}

object BookedSlotsRepository {
  case class Slot(slotId: SlotId, dayId: DayId, month: Month)
}
