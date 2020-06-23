package net.domaincentric.scheduling.application.eventsourcing

case class Version(value: Long) {
  def isNew: Boolean                       = value == -1L
  def incrementBy(increment: Int): Version = new Version(value + increment.toLong)
  def nextReadVersion: Version             = Version(value + 1L)
}

object Version {
  val `new`: Version = new Version(-1L)

  implicit def longToVersion(value: Long): Version   = new Version(value)
  implicit def versionToLong(version: Version): Long = version.value
}
