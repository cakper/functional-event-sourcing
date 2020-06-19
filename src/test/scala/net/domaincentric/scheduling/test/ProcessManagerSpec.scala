package net.domaincentric.scheduling.test

import java.time.{ Clock, Instant, ZoneOffset }
import java.util.UUID

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import net.domaincentric.scheduling.application.eventsourcing.{ EventHandler, EventMetadata }
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

abstract class ProcessManagerSpec extends AsyncWordSpec with Matchers {
  implicit val uuidGenerator: ReplayableUuidGenerator = new ReplayableUuidGenerator()
  implicit val clock: Clock                           = Clock.fixed(Instant.now(), ZoneOffset.UTC)

  def handler: EventHandler
  var metadata: EventMetadata = _

  private def genMetadata() =
    EventMetadata(uuidGenerator.next().toString, uuidGenerator.next().toString, replayed = true)

  var nextPosition = 0L

  def `given`(events: Any*): Unit = {
    Await.result(
      Task
        .traverse(events.zipWithIndex) {
          case (event, position) =>
            handler.handle(event, genMetadata(), uuidGenerator.next(), position.toLong, Instant.now())
        }
        .map { events =>
          nextPosition = events.length.toLong
        }
        .runToFuture,
      Duration.Inf
    )
    metadata = genMetadata()
    uuidGenerator.reset()
  }

  def `when`(event: Any): Unit = {
    val uuid = uuidGenerator.next()
    uuidGenerator.reset()
    Await.result(
      handler.handle(event, metadata, uuid, nextPosition, Instant.now()).runToFuture,
      Duration.Inf
    )
  }

  def `then`[A](assertion: Task[Assertion]): Future[Assertion] = {
    uuidGenerator.replay()
    assertion.runToFuture.map { assertion =>
      uuidGenerator.reset()
      assertion
    }
  }
}
