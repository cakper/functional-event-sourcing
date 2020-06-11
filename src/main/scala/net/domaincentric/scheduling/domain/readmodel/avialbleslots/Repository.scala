package net.domaincentric.scheduling.domain.readmodel.avialbleslots

import java.time.LocalDate
import java.util.UUID

import monix.eval.Task

trait Repository {
  def addSlot(availableSlot: AvailableSlot): Task[Unit]
  def hideSlot(slotId: UUID): Task[Unit]
  def showSlot(slotId: UUID): Task[Unit]
  def getAvailableSlotsOn(date: LocalDate): Task[Seq[AvailableSlot]]
}
