package net.domaincentric.scheduling.eventsourcing

import java.util.UUID

trait Event {
  def eventId: UUID
}
