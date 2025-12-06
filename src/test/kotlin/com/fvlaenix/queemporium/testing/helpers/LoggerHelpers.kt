package com.fvlaenix.queemporium.testing.helpers

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fvlaenix.queemporium.commands.LoggerMessageCommand
import org.slf4j.LoggerFactory

class LoggerTestContext {
  private val logger: Logger = LoggerFactory.getLogger(LoggerMessageCommand::class.java) as Logger
  private val listAppender: ListAppender<ILoggingEvent> = ListAppender()

  init {
    listAppender.start()
    logger.addAppender(listAppender)
  }

  fun clearLogs() {
    listAppender.list.clear()
  }

  fun getLogsContaining(text: String): List<ILoggingEvent> {
    return listAppender.list.filter { it.formattedMessage.contains(text) }
  }

  fun getAllLogs(): List<ILoggingEvent> {
    return listAppender.list.toList()
  }

  fun cleanup() {
    logger.detachAppender(listAppender)
    listAppender.stop()
  }
}
