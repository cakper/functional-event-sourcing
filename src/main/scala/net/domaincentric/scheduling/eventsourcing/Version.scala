package net.domaincentric.scheduling.eventsourcing

class Version(val value: Long) extends AnyVal {
  def isNew: Boolean                       = value == -1L
  def incrementBy(increment: Int): Version = new Version(value + increment.toLong)
}

object Version {
  val `new`: Version = new Version(-1L)
}
