package net.domaincentric.scheduling.application.eventsourcing

import net.domaincentric.scheduling.domain.service.UuidGenerator

case class CausationId(value: String)

object CausationId {
  def create(implicit uuidGenerator: UuidGenerator): CausationId = new CausationId(uuidGenerator.next().toString)
}
