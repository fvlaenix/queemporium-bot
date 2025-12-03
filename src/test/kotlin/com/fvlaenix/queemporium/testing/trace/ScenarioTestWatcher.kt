package com.fvlaenix.queemporium.testing.trace

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestWatcher
import org.slf4j.MDC
import java.io.File
import java.util.*

class ScenarioTestWatcher : TestWatcher, BeforeTestExecutionCallback {

  override fun beforeTestExecution(context: ExtensionContext) {
    val testId = ScenarioTraceCollector.startTrace(context.displayName)
    // Store testId in ExtensionContext to retrieve it later reliably
    getStore(context).put(ScenarioTraceCollector.TEST_ID_KEY, testId)
  }

  override fun testDisabled(context: ExtensionContext, reason: Optional<String>) {
    cleanup(context)
  }

  override fun testSuccessful(context: ExtensionContext) {
    cleanup(context)
  }

  override fun testAborted(context: ExtensionContext, cause: Throwable?) {
    handleFailure(context, cause)
  }

  override fun testFailed(context: ExtensionContext, cause: Throwable?) {
    handleFailure(context, cause)
  }

  private fun handleFailure(context: ExtensionContext, cause: Throwable?) {
    // Restore MDC from store if needed (though ScenarioTraceCollector.getTrace uses MDC directly)
    // If MDC was cleared, we can restore it using the stored ID
    val testId = getStore(context).get(ScenarioTraceCollector.TEST_ID_KEY) as? String
    if (testId != null) {
      MDC.put(ScenarioTraceCollector.TEST_ID_KEY, testId)
    }

    val trace = ScenarioTraceCollector.getTrace()
    if (trace != null) {
      val reportDir = File("build/reports/scenarios/${context.requiredTestClass.simpleName}")
      ScenarioReportWriter.writeReport(
        context.displayName,
        cause,
        trace,
        reportDir
      )
    }
    cleanup(context)
  }

  private fun cleanup(context: ExtensionContext) {
    val testId = getStore(context).get(ScenarioTraceCollector.TEST_ID_KEY) as? String
    if (testId != null) {
      MDC.put(ScenarioTraceCollector.TEST_ID_KEY, testId)
    }
    ScenarioTraceCollector.clear()
  }

  private fun getStore(context: ExtensionContext): ExtensionContext.Store {
    return context.getStore(ExtensionContext.Namespace.create(javaClass, context.requiredTestMethod))
  }
}
