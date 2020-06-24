package net.domaincentric.scheduling.infrastructure.mongodb

import java.time.LocalDate

import monix.eval.Task
import net.domaincentric.scheduling.domain.readmodel.archivableday
import net.domaincentric.scheduling.domain.writemodel.doctorday.DayId
import org.bson.codecs.configuration.CodecRegistries.{ fromProviders, fromRegistries }
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.{ MongoCollection, MongoDatabase }

class ArchivableDaysRepository(database: MongoDatabase) extends archivableday.ArchivableDaysRepository {
  case class Row(_id: ObjectId, date: LocalDate, dayId: DayId)

  private val codecRegistry = fromRegistries(fromProviders(classOf[Row], classOf[DayId]), DEFAULT_CODEC_REGISTRY)

  private val archivableDays: MongoCollection[Row] =
    database.withCodecRegistry(codecRegistry).getCollection("archivable_days")

  override def add(date: LocalDate, dayId: DayId): Task[Unit] =
    Task
      .deferFuture {
        archivableDays.insertOne(Row(new ObjectId(), date, dayId)).toFuture()
      }
      .map(_ => ())

  override def remove(dayId: DayId): Task[Unit] =
    Task
      .deferFuture {
        archivableDays
          .deleteOne(equal("dayId", dayId))
          .toFuture()
      }
      .map(_ => ())

  override def find(date: LocalDate): Task[Seq[DayId]] =
    Task
      .deferFuture {
        archivableDays
          .find(equal("date", date))
          .toFuture()
      }
      .map(_.map(_.dayId))

}
