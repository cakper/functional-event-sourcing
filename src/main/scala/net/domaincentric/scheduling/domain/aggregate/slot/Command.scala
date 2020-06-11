package net.domaincentric.scheduling.domain.aggregate.slot

import java.time.LocalDateTime

import scala.concurrent.duration.Duration

sealed trait Command

case class Schedule(startTime: LocalDateTime, duration: Duration) extends Command
case class Book(patientId: String)                                extends Command
case class Cancel(reason: String)                                 extends Command
