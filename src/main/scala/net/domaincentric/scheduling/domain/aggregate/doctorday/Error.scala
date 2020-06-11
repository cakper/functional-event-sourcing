package net.domaincentric.scheduling.domain.aggregate.doctorday

sealed trait Error

object DayAlreadyScheduled extends Error
object SlotNotScheduled    extends Error
object SlotNotBooked       extends Error
object SlotOverlapped      extends Error
object SlotAlreadyBooked   extends Error
