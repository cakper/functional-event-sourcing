package net.domaincentric.scheduling.domain.writemodel.calendar

import java.time.LocalDate

trait Event

case class CalendarDayStarted(date: LocalDate) extends Event
