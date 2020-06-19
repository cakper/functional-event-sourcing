package net.domaincentric.scheduling.infrastructure.mongodb

case class UnableToFindSlot(message: String) extends RuntimeException(message) {}
