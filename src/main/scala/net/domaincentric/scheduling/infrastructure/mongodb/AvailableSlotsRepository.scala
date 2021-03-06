package net.domaincentric.scheduling.infrastructure.mongodb

import java.time.LocalDate

import monix.eval.Task
import net.domaincentric.scheduling.domain.writemodel.doctorday.{ DayId, DoctorId, SlotId }
import net.domaincentric.scheduling.domain.readmodel.avialbleslots
import net.domaincentric.scheduling.domain.readmodel.avialbleslots.{ AvailableSlot, AvailableSlotsRepository }
import org.bson.codecs.configuration.CodecRegistries.{ fromProviders, fromRegistries }
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Filters.{ and, equal }
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.{ MongoCollection, MongoDatabase }

class AvailableSlotsRepository(database: MongoDatabase) extends avialbleslots.AvailableSlotsRepository {
  case class SlotRow(_id: ObjectId, data: AvailableSlot, hidden: Boolean)

  private val codecRegistry =
    fromRegistries(
      fromProviders(classOf[SlotRow], classOf[AvailableSlot], classOf[DayId], classOf[SlotId], classOf[DoctorId]),
      DEFAULT_CODEC_REGISTRY
    )

  private val collection: MongoCollection[SlotRow] =
    database.withCodecRegistry(codecRegistry).getCollection("available_slots")

  override def addSlot(availableSlot: AvailableSlot): Task[Unit] =
    Task
      .deferFuture(
        collection
          .insertOne(SlotRow(new ObjectId(), availableSlot, hidden = false))
          .toFuture()
      )
      .map(_ => ())

  override def getAvailableSlotsOn(date: LocalDate): Task[Seq[AvailableSlot]] =
    Task
      .deferFuture(collection.find(and(equal("data.date", date), equal("hidden", false))).toFuture())
      .map(_.map(_.data))

  override def hideSlot(slotId: SlotId): Task[Unit] = setStatus(slotId, hidden = true)

  override def showSlot(slotId: SlotId): Task[Unit] = setStatus(slotId, hidden = false)

  private def setStatus(slotId: SlotId, hidden: Boolean) =
    Task
      .deferFuture(
        collection.findOneAndUpdate(equal("data.slotId", slotId), set("hidden", hidden)).toFuture()
      )
      .map(_ => ())

  override def deleteSlots(dayId: DayId): Task[Unit] =
    Task
      .deferFuture(collection.deleteMany(equal("data.dayId", dayId)).toFuture())
      .map(_ => ())
}
