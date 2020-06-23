package net.domaincentric.scheduling.application.eventsourcing

import net.domaincentric.scheduling.domain.service.UuidGenerator

case class CorrelationId(value: String)

object CorrelationId {
  def create(implicit uuidGenerator: UuidGenerator): CorrelationId = new CorrelationId(uuidGenerator.next().toString)
}
