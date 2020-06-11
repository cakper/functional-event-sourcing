package net.domaincentric.scheduling.domain.aggregate.slot

sealed trait Error

case class SlotAlreadyScheduled() extends Error
case class SlotNotScheduled()     extends Error
case class SlotAlreadyBooked()    extends Error
case class SlotNotBooked()        extends Error
case class SlotAlreadyStarted()   extends Error
