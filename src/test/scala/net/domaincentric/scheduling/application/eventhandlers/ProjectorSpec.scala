package net.domaincentric.scheduling.application.eventhandlers

import java.time.Instant
import java.util.UUID

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import net.domaincentric.scheduling.application.eventsourcing.{ EventHandler, EventMetadata }
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

abstract class ProjectorSpec extends AsyncWordSpec with Matchers {
  def handler: EventHandler
  val metadata: EventMetadata = EventMetadata("abc", "123")

  def `given`(events: Any*): Unit = {
    Await.result(
      Task
        .traverse(events) { event =>
          handler.handle(event, metadata, UUID.randomUUID(), 0L, Instant.now())
        }
        .runToFuture,
      Duration.Inf
    )
  }

  def `then`[A](actualResultT: Task[A], expected: A): Future[Assertion] =
    actualResultT.map(_ shouldEqual expected).runToFuture
}
