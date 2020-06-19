package net.domaincentric.scheduling.test

import org.mongodb.scala.{ MongoClient, MongoDatabase }
import org.scalatest.{ BeforeAndAfterEach, Suite }

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait MongoDatabaseSpec extends BeforeAndAfterEach {
  this: Suite =>
  val database: MongoDatabase = MongoClient("mongodb://localhost").getDatabase("projections")

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    Await.result(database.drop().toFuture(), Duration.Inf)
  }
}
