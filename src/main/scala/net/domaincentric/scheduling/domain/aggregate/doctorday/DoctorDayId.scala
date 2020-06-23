package net.domaincentric.scheduling.domain.aggregate.doctorday

import java.time.LocalDate

import net.domaincentric.scheduling.application.eventsourcing.AggregateId

object DoctorDayId {
  def apply(dayId: DayId): AggregateId                        = AggregateId(dayId.toString, "doctorday")
  def apply(doctorId: DoctorId, date: LocalDate): AggregateId = apply(DayId(doctorId, date))

  def unapply(str: String): Option[AggregateId] = {
    if (str.nonEmpty) Some(apply(DayId(str)))
    else None
  }
}
