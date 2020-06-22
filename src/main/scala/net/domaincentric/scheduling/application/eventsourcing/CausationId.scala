package net.domaincentric.scheduling.application.eventsourcing

import java.util.UUID

import net.domaincentric.scheduling.domain.service.UuidGenerator

case class CausationId(value: String)

object CausationId {
  def create(implicit uuidGenerator: UuidGenerator): CausationId = new CausationId(uuidGenerator.toString)
}
