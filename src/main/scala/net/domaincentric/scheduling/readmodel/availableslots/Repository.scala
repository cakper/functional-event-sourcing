package net.domaincentric.scheduling.readmodel.availableslots

import java.time.LocalDate

class Repository {
  def getAvailableSlotsOn(date: LocalDate): Seq[AvailableSlot] = ???
}
