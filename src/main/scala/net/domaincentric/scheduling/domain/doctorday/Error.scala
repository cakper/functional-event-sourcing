package net.domaincentric.scheduling.domain.doctorday

import net.domaincentric.scheduling.eventsourcing.{ Error => ESError }

sealed trait Error extends ESError

object DayAlreadyScheduled extends Error
object SlotNotScheduled    extends Error
object SlotNotBooked       extends Error
object SlotOverlapped      extends Error
object SlotAlreadyBooked   extends Error
