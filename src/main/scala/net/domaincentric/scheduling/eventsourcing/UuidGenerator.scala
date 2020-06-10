package net.domaincentric.scheduling.eventsourcing

import java.util.UUID

trait UuidGenerator {
  def next(): UUID
}
