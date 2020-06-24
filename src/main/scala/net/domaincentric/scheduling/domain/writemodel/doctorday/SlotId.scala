package net.domaincentric.scheduling.domain.writemodel.doctorday

import java.util.UUID

import net.domaincentric.scheduling.domain.service.UuidGenerator

case class SlotId(value: UUID) {
  override def toString: String = value.toString
}

object SlotId {
  def create(implicit uuidGenerator: UuidGenerator): SlotId = SlotId(uuidGenerator.next())
}
