package net.domaincentric.scheduling.domain.writemodel.doctorday

import java.time.LocalDate

case class DayId(value: String) {
  override def toString: String = value
}

object DayId {
  def apply(doctorId: DoctorId, date: LocalDate): DayId = DayId(s"${doctorId}_$date")
}
