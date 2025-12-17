package com.fvlaenix.queemporium.testing.log

import ch.qos.logback.classic.Level

data class ExpectedLog(
  val level: Level,
  val logger: String,
  val count: Int,
  val messageContains: String? = null
) {
  fun matches(capturedLog: CapturedLogEvent): Boolean {
    if (capturedLog.level != level) return false
    if (capturedLog.loggerName != logger) return false
    if (messageContains != null && !capturedLog.message.contains(messageContains)) return false
    return true
  }

  override fun toString(): String {
    val messageStr = messageContains?.let { " with message containing '$it'" } ?: ""
    return "$level from $logger (count=$count)$messageStr"
  }
}

class ExpectedLogsBuilder {
  private val expectedLogs = mutableListOf<ExpectedLog>()

  fun error(logger: String, count: Int = 1, messageContains: String? = null) {
    expectedLogs.add(ExpectedLog(Level.ERROR, logger, count, messageContains))
  }

  fun warn(logger: String, count: Int = 1, messageContains: String? = null) {
    expectedLogs.add(ExpectedLog(Level.WARN, logger, count, messageContains))
  }

  internal fun build(): List<ExpectedLog> = expectedLogs.toList()
}

object ExpectedLogsContext {
  private val expectedLogsByTest = ThreadLocal<List<ExpectedLog>>()

  fun setExpectedLogs(logs: List<ExpectedLog>) {
    expectedLogsByTest.set(logs)
  }

  fun getExpectedLogs(): List<ExpectedLog> {
    return expectedLogsByTest.get() ?: emptyList()
  }

  fun clear() {
    expectedLogsByTest.remove()
  }
}

fun expectLogs(block: ExpectedLogsBuilder.() -> Unit) {
  val builder = ExpectedLogsBuilder()
  builder.block()
  ExpectedLogsContext.setExpectedLogs(builder.build())
}
