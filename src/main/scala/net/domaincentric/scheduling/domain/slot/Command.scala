package net.domaincentric.scheduling.domain.slot

import java.time.LocalDateTime

import net.domaincentric.scheduling.eventsourcing.{Command => ESCommand}

import scala.concurrent.duration.Duration

sealed trait Command extends ESCommand

case class Schedule(startTime: LocalDateTime, duration: Duration) extends Command
case class Book(patientId: String)                                extends Command
case class Cancel(reason: String)                                 extends Command
