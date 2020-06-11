package net.domaincentric.scheduling.domain.service

import java.util.UUID

trait UuidGenerator {
  def next(): UUID
}
