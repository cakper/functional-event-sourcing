//package net.domaincentric.scheduling.readmodel.avialbleslots
//
//import java.time.{ Clock, Instant, LocalDate, LocalDateTime, LocalTime, ZoneOffset }
//import java.util.UUID
//
//import net.domaincentric.scheduling.domain.doctorday.SlotScheduled
//import net.domaincentric.scheduling.eventsourcing.{ Event, EventHandler }
//import net.domaincentric.scheduling.readmodel.availableslots._
//import org.scalatest.Assertion
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//
//import scala.concurrent.duration._
//
//class ProjectorSpec extends AnyWordSpec with Matchers {
//  def randomId(): UUID      = UUID.randomUUID()
//  implicit val clock: Clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)
//
//  private val today: LocalDate           = LocalDate.now(clock)
//  private val tenAm: LocalTime           = LocalTime.of(10, 0)
//  private val tenAmToday: LocalDateTime  = LocalDateTime.of(today, tenAm)
//  private val tenMinutes: FiniteDuration = 10.minutes
//
//  private val repository: Repository = ???
//  private val handler: EventHandler  = ???
//
//  def `given`(events: Event*) = {
//
//    List.of(events).forEach(event -> handler.handle(event, null))
//
//  }
//
//  def `then`[A](actual: A, expected: A): Assertion = expected shouldEqual actual
//
//  "available slots projector" should {
//    "handle scheduling a slot" in {
//      val scheduled = SlotScheduled(randomId(), randomId(), tenAmToday, tenMinutes)
//      `given`(scheduled)
//      `then`(
//        repository.getAvailableSlotsOn(today),
//        Seq(AvailableSlot(scheduled.dayPlannedEventId, scheduled.eventId, scheduled.startTime, scheduled.duration))
//      )
//    }
//  }
//
//}
//
////class AvailableSlotsProjectorTest extends ProjectorTest {
////  private var repository = null
////
////  @BeforeEach private[readmodel] def beforeEach(): Unit = {
////    repository = new InMemoryAvailableSlotsRepository
////    handler = new AvailableSlotsProjector(repository)
////  }
////
////  @Test private[readmodel] def shouldAddSlotToTheList(): Unit = {
////    val scheduled = new Scheduled(randomUuid, LocalDateTime.now, Duration.ofMinutes(10L))
////    `given`(scheduled)
////    `then`(List.of(new AvailableSlot(scheduled.getEventId, scheduled.getStartTime, scheduled.getDuration)), repository.getSlotsAvailableOn(LocalDate.now))
////  }
////
////  @Test private[readmodel] def shouldRemoveSlotFromTheListIfItWasBooked(): Unit = {
////    val scheduled = new Scheduled(randomUuid, LocalDateTime.now, Duration.ofMinutes(10L))
////    val booked = new Booked(randomUuid, scheduled.getEventId, randomString)
////    `given`(scheduled, booked)
////    `then`(List.empty, repository.getSlotsAvailableOn(LocalDate.now))
////  }
////
////  @Test private[readmodel] def shouldAddSlotAgainIfBookingWasCancelled(): Unit = {
////    val scheduled = new Scheduled(randomUuid, LocalDateTime.now, Duration.ofMinutes(10L))
////    val booked = new Booked(randomUuid, scheduled.getEventId, randomString)
////    val cancelled = new Cancelled(randomUuid, scheduled.getEventId, randomString)
////    `given`(scheduled, booked, cancelled)
////    `then`(List.of(new AvailableSlot(scheduled.getEventId, scheduled.getStartTime, scheduled.getDuration)), repository.getSlotsAvailableOn(LocalDate.now))
////  }
////}
