package net.domaincentric.scheduling.domain.service

import java.util.UUID

trait UuidGenerator {
  def next(): UUID
}

object RandomUuidGenerator extends UuidGenerator {
  def next(): UUID = UUID.randomUUID()
}
