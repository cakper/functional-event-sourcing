package net.domaincentric.scheduling.domain.readmodel.avialbleslots

import java.time.LocalDate

import monix.eval.Task
import net.domaincentric.scheduling.domain.aggregate.doctorday.{ DayId, SlotId }

trait Repository {
  def addSlot(availableSlot: AvailableSlot): Task[Unit]
  def hideSlot(slotId: SlotId): Task[Unit]
  def showSlot(slotId: SlotId): Task[Unit]
  def getAvailableSlotsOn(date: LocalDate): Task[Seq[AvailableSlot]]
  def deleteSlots(dayId: DayId): Task[Unit]
}
