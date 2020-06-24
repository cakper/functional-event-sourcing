package net.domaincentric.scheduling.domain.readmodel.archivableday

import java.time.LocalDate

import monix.eval.Task
import net.domaincentric.scheduling.domain.writemodel.doctorday.DayId

trait ArchivableDaysRepository {
  def add(date: LocalDate, dayId: DayId): Task[Unit]
  def remove(dayId: DayId): Task[Unit]
  def find(date: LocalDate): Task[Seq[DayId]]
}
