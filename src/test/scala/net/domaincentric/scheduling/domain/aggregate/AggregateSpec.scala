package net.domaincentric.scheduling.domain.aggregate

import java.time.{ Clock, Instant, ZoneOffset }
import java.util.UUID

import net.domaincentric.scheduling.application.eventsourcing.{ Aggregate, AggregateId }
import net.domaincentric.scheduling.test.ReplayableUuidGenerator
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{ Assertion, BeforeAndAfterEach }

trait AggregateSpec[C, E, Er, S <: State[S, E]] extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  implicit val clock: Clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)
  implicit val uuidGenerator: ReplayableUuidGenerator =
    new ReplayableUuidGenerator()
  var result: Either[Er, Aggregate[C, E, Er, S]] = _
  var aggregate: Aggregate[C, E, Er, S]          = _

  def state(): S

  def handler(): CommandHandler[C, E, Er, S]

  def `given`(events: E*): Unit = {
    aggregate = aggregate.reconstitute(events)
  }

  def `when`(command: C): Unit = {
    uuidGenerator.reset()
    result = aggregate.handle(command)
    uuidGenerator.replay()
  }

  def `then`(events: E*): Assertion =
    result match {
      case Left(error) =>
        throw new RuntimeException(s"Expected $events but got $error")
      case Right(aggregate) =>
        aggregate.changes shouldEqual events
        aggregate.markAsCommitted.version shouldEqual aggregate.version.incrementBy(aggregate.changes.length)
    }

  def `then`(expected: Er): Assertion = result match {
    case Left(error) => expected shouldEqual error
    case Right(aggregate) =>
      throw new RuntimeException(s"Expected $expected but got $aggregate")
  }

  protected def nextUuid(): UUID = {
    uuidGenerator.next()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    uuidGenerator.reset()
    aggregate = new Aggregate[C, E, Er, S](AggregateId(randomString(), "test"), state(), handler())
  }

  protected def randomString(): String = UUID.randomUUID().toString
}
