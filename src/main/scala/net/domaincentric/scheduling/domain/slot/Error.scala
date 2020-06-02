package net.domaincentric.scheduling.domain.slot

import net.domaincentric.scheduling.eventsourcing.{Error => ESError}

sealed trait Error extends ESError

case class SlotAlreadyScheduled() extends Error
case class SlotNotScheduled()     extends Error
case class SlotAlreadyBooked()    extends Error
case class SlotNotBooked()        extends Error
case class SlotAlreadyStarted()   extends Error
