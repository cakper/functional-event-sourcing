package net.domaincentric.scheduling.eventsourcing

import java.time.{ Clock, Instant, ZoneOffset }
import java.util.UUID

import net.domaincentric.scheduling.eventsourcing.Aggregate.Handler
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{ Assertion, BeforeAndAfterEach }

trait AggregateSpec[C <: Command, E <: Event, Er <: Error, S <: State[S, E]]
    extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  implicit val clock: Clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)
  implicit val uuidGenerator: ReplayableUuidGenerator =
    new ReplayableUuidGenerator()
  var result: Either[Error, Aggregate[C, E, Er, S]] = _
  var aggregate: Aggregate[C, E, Er, S]             = _

  def state(): S

  def handler(): Handler[C, E, Er, S]

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
        events shouldEqual aggregate.changes
        aggregate.markAsCommitted.version shouldEqual aggregate.version.incrementBy(aggregate.changes.length)
    }

  def `then`(expected: Er): Assertion = result match {
    case Left(error) => expected shouldEqual error
    case Right(aggregate) =>
      throw new RuntimeException(s"Expected $expected but got $aggregate")
  }

  protected def randomId(): UUID = {
    uuidGenerator.nextUuid()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    uuidGenerator.reset()
    aggregate = new Aggregate[C, E, Er, S](randomString, state(), handler())
  }

  protected def randomString: String = UUID.randomUUID().toString
}
