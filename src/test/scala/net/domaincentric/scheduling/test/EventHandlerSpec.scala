package net.domaincentric.scheduling.test

import java.time.{ Clock, Instant, ZoneOffset }

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import net.domaincentric.scheduling.application.eventsourcing.CommandBus.CommandEnvelope
import net.domaincentric.scheduling.application.eventsourcing.{ CausationId, CommandBus, CorrelationId, EventHandler, EventMetadata }
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.util.control.NonFatal

abstract class EventHandlerSpec extends AsyncWordSpec with Matchers {
  def enableAtLeastOnceMonkey: Boolean = false
  def enableWonkyIoMonkey: Boolean     = false

  def handler: EventHandler

  private val inMemoryCommandBus: InMemoryCommandBus = new InMemoryCommandBus()
  val commandBus: CommandBus                         = inMemoryCommandBus

  implicit val uuidGenerator: ReplayableUuidGenerator = new ReplayableUuidGenerator()
  implicit val clock: Clock                           = Clock.fixed(Instant.now(), ZoneOffset.UTC)

  var metadata: EventMetadata = _

  private def genMetadata() =
    EventMetadata(CorrelationId.create, CausationId.create, Some("123"))

  private var nextPosition = 0L

  private val eventRepetitions: Int = if (enableAtLeastOnceMonkey) 2 else 1

  def `given`(events: Any*): Unit = {
    Await.result(
      Task
        .traverse(events.zipWithIndex) {
          case (event, position) =>
            val range: Seq[Int] = 0 until eventRepetitions
            Task
              .traverse(range) { _ =>
                handler.handle(event, genMetadata(), uuidGenerator.next(), position.toLong, Instant.now())
              }
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
    if (enableWonkyIoMonkey) inMemoryCommandBus.enableMonkey()
    Await.result(
      handler
        .handle(event, metadata, uuid, nextPosition, Instant.now())
        .onErrorRestartIf {
          case NonFatal(_) =>
            inMemoryCommandBus.disableMonkey()
            true
          case _ => false
        }
        .runToFuture,
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

  def `then`[A](assertionT: Seq[CommandEnvelope] => Task[Assertion]): Future[Assertion] = {
    uuidGenerator.replay()
    inMemoryCommandBus
      .subscribe()
      .toListL
      .flatMap { commands =>
        assertionT(commands).map { assertion =>
          uuidGenerator.reset()
          inMemoryCommandBus.reset()
          assertion
        }
      }
      .runToFuture
  }
}
