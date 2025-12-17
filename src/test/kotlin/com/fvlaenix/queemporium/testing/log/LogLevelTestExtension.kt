package com.fvlaenix.queemporium.testing.log

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class LogLevelTestExtension : BeforeEachCallback, AfterEachCallback {

  override fun beforeEach(context: ExtensionContext) {
    TestLogCapture.clearCapturedLogs()
    ExpectedLogsContext.clear()
  }

  override fun afterEach(context: ExtensionContext) {
    val actualLogs = TestLogCapture.getWarnAndErrorLogs()
    val expectedLogs = ExpectedLogsContext.getExpectedLogs()

    if (expectedLogs.isEmpty()) {
      if (actualLogs.isNotEmpty()) {
        throw AssertionError(buildUnexpectedLogsMessage(actualLogs))
      }
    } else {
      validateExpectedLogs(expectedLogs, actualLogs)
    }

    ExpectedLogsContext.clear()
  }

  private fun buildUnexpectedLogsMessage(actualLogs: List<CapturedLogEvent>): String {
    return buildString {
      appendLine("Test failed due to unexpected WARN or ERROR logs:")
      appendLine()
      actualLogs.forEach { log ->
        appendLine(log.toString())
        appendLine()
      }
      appendLine("Total unexpected WARN/ERROR logs: ${actualLogs.size}")
      appendLine()
      appendLine("If these logs are expected, use expectLogs { ... } to declare them.")
    }
  }

  private fun validateExpectedLogs(expectedLogs: List<ExpectedLog>, actualLogs: List<CapturedLogEvent>) {
    val errors = mutableListOf<String>()

    for (expected in expectedLogs) {
      val matchingLogs = actualLogs.filter { expected.matches(it) }
      val actualCount = matchingLogs.size

      if (actualCount != expected.count) {
        errors.add(buildCountMismatchMessage(expected, actualCount, matchingLogs))
      }
    }

    val unmatchedLogs = actualLogs.filter { actualLog ->
      expectedLogs.none { expected -> expected.matches(actualLog) }
    }

    if (unmatchedLogs.isNotEmpty()) {
      errors.add(buildUnmatchedLogsMessage(unmatchedLogs))
    }

    if (errors.isNotEmpty()) {
      throw AssertionError(buildValidationErrorMessage(expectedLogs, actualLogs, errors))
    }
  }

  private fun buildCountMismatchMessage(
    expected: ExpectedLog,
    actualCount: Int,
    matchingLogs: List<CapturedLogEvent>
  ): String {
    return buildString {
      appendLine("Expected: $expected")
      appendLine("Actual count: $actualCount")
      if (matchingLogs.isNotEmpty()) {
        appendLine("Matching logs:")
        matchingLogs.forEach { log ->
          appendLine("  - ${log.message}")
        }
      }
    }
  }

  private fun buildUnmatchedLogsMessage(unmatchedLogs: List<CapturedLogEvent>): String {
    return buildString {
      appendLine("Unexpected logs (not matching any expectation):")
      unmatchedLogs.forEach { log ->
        appendLine("  - [$log.level] ${log.loggerName}: ${log.message}")
      }
    }
  }

  private fun buildValidationErrorMessage(
    expectedLogs: List<ExpectedLog>,
    actualLogs: List<CapturedLogEvent>,
    errors: List<String>
  ): String {
    return buildString {
      appendLine("Test failed: Expected logs do not match actual logs")
      appendLine()
      appendLine("=".repeat(80))
      appendLine("EXPECTED LOGS:")
      appendLine("=".repeat(80))
      expectedLogs.forEach { expected ->
        appendLine("  - $expected")
      }
      appendLine()
      appendLine("=".repeat(80))
      appendLine("ACTUAL LOGS (${actualLogs.size} total):")
      appendLine("=".repeat(80))
      actualLogs.forEach { log ->
        appendLine("  - [${log.level}] ${log.loggerName}: ${log.message}")
      }
      appendLine()
      appendLine("=".repeat(80))
      appendLine("VALIDATION ERRORS:")
      appendLine("=".repeat(80))
      errors.forEach { error ->
        appendLine(error)
        appendLine()
      }
    }
  }
}
