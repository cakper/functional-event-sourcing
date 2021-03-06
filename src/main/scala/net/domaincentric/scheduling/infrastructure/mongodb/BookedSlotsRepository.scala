package net.domaincentric.scheduling.infrastructure.mongodb

import java.time.Month

import monix.eval.Task
import net.domaincentric.scheduling.domain.readmodel.bookedslots
import net.domaincentric.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository.Slot
import net.domaincentric.scheduling.domain.writemodel.doctorday.{ DayId, DoctorId, PatientId, SlotId }
import org.bson.codecs.configuration.CodecRegistries.{ fromProviders, fromRegistries }
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Filters.{ and, equal }
import org.mongodb.scala.{ MongoCollection, MongoDatabase }

class BookedSlotsRepository(database: MongoDatabase) extends bookedslots.BookedSlotsRepository {
  case class SlotDateRow(_id: ObjectId, slotId: SlotId, dayId: DayId, monthNumber: Int) {
    def month: Month = Month.of(monthNumber)
  }
  case class PatientSlotRow(_id: ObjectId, patientId: PatientId, monthNumber: Int, slotId: SlotId) {
    def month: Month = Month.of(monthNumber)
  }

  private val codecRegistry =
    fromRegistries(
      fromProviders(
        classOf[SlotDateRow],
        classOf[DayId],
        classOf[SlotId],
        classOf[PatientSlotRow],
        classOf[DoctorId],
        classOf[PatientId]
      ),
      DEFAULT_CODEC_REGISTRY
    )

  private val slotMonths: MongoCollection[SlotDateRow] =
    database.withCodecRegistry(codecRegistry).getCollection("slot_dates")

  private val patientSlots: MongoCollection[PatientSlotRow] =
    database.withCodecRegistry(codecRegistry).getCollection("patient_slots")

  override def countByPatientAndMonth(patientId: PatientId, month: Month): Task[Int] =
    Task
      .deferFuture {
        patientSlots
          .countDocuments(
            and(equal("patientId", patientId), equal("monthNumber", month.getValue))
          )
          .toFuture()
      }
      .map(_.toInt)

  override def addSlot(slot: Slot): Task[Unit] =
    Task
      .deferFuture {
        slotMonths
          .insertOne(SlotDateRow(new ObjectId(), slot.slotId, slot.dayId, slot.month.getValue))
          .toFuture()
      }
      .map(_ => ())

  override def markSlotAsBooked(slotId: SlotId, patientId: PatientId): Task[Unit] =
    for {
      slot <- getSlot(slotId)
      _ <- Task
        .deferFuture {
          patientSlots
            .insertOne(PatientSlotRow(new ObjectId(), patientId, slot.month.getValue, slotId))
            .toFuture()
        }
    } yield ()

  override def markSlotAsAvailable(slotId: SlotId): Task[Unit] =
    Task
      .deferFuture {
        patientSlots
          .deleteOne(equal("slotId", slotId))
          .toFuture()
      }
      .map(_ => ())

  override def getSlot(slotId: SlotId): Task[Slot] = {
    Task
      .deferFuture {
        slotMonths.find(equal("slotId", slotId)).toFuture()
      }
      .map(_.headOption)
      .flatMap {
        case None       => Task.raiseError(UnableToFindSlot(s"Slot date for slot with id: $slotId not found"))
        case Some(slot) => Task.now(Slot(slot.slotId, slot.dayId, slot.month))
      }
  }

  override def findAllSlotIdsFor(patientId: PatientId): Task[Seq[SlotId]] =
    Task
      .deferFuture {
        patientSlots
          .find(equal("patientId", patientId))
          .toFuture()
      }
      .map(_.map(_.slotId))
}

case class UnableToFindSlot(message: String) extends RuntimeException(message) {}
