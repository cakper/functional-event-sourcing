package net.domaincentric.scheduling.test

import java.util.UUID

import net.domaincentric.scheduling.domain.service.UuidGenerator

class ReplayableUuidGenerator() extends UuidGenerator {
  private var replayMode: Boolean = false
  private var ids: List[UUID]     = List.empty

  override def next(): UUID = {
    if (replayMode) {
      if (ids.isEmpty) {
        throw new RuntimeException("Replayable UUID generator doesn't have any ids left to replay")
      }
      val next: UUID = ids.head
      ids = ids.tail
      next
    } else {
      val id: UUID = UUID.randomUUID
      ids = ids.appended(id)
      id
    }
  }

  def replay(): Unit = {
    replayMode = true
  }

  def reset(): Unit = {
    replayMode = false
    ids = List.empty
  }
}
