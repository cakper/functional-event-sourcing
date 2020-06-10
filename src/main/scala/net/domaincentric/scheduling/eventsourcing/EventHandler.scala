package net.domaincentric.scheduling.eventsourcing

trait EventHandler {
  def handle[E <: Event](event: E, metadata: EventMetadata): Unit
}
