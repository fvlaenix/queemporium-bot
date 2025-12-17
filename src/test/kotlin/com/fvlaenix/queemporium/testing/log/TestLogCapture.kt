package com.fvlaenix.queemporium.testing.log

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import java.util.concurrent.ConcurrentLinkedQueue

data class CapturedLogEvent(
  val level: Level,
  val loggerName: String,
  val message: String,
  val throwable: Throwable?
) {
  override fun toString(): String {
    val throwableStr = throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
    return "[$level] $loggerName - $message$throwableStr"
  }
}

class TestLogCapture : AppenderBase<ILoggingEvent>() {

  companion object {
    private val capturedLogs = ConcurrentLinkedQueue<CapturedLogEvent>()

    fun getCapturedLogs(): List<CapturedLogEvent> {
      return capturedLogs.toList()
    }

    fun clearCapturedLogs() {
      capturedLogs.clear()
    }

    fun getWarnAndErrorLogs(packagePrefix: String = "com.fvlaenix.queemporium"): List<CapturedLogEvent> {
      return capturedLogs.filter { event ->
        (event.level == Level.WARN || event.level == Level.ERROR) &&
            event.loggerName.startsWith(packagePrefix)
      }
    }
  }

  override fun append(eventObject: ILoggingEvent) {
    val capturedEvent = CapturedLogEvent(
      level = eventObject.level,
      loggerName = eventObject.loggerName,
      message = eventObject.formattedMessage,
      throwable = eventObject.throwableProxy?.let {
        val throwable = RuntimeException(it.message)
        throwable.stackTrace = it.stackTraceElementProxyArray.map { proxy ->
          StackTraceElement(
            proxy.stackTraceElement.className,
            proxy.stackTraceElement.methodName,
            proxy.stackTraceElement.fileName,
            proxy.stackTraceElement.lineNumber
          )
        }.toTypedArray()
        throwable
      }
    )

    capturedLogs.add(capturedEvent)
  }
}
