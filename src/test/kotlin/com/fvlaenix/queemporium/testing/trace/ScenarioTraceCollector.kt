package com.fvlaenix.queemporium.testing.trace

import org.slf4j.MDC
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ScenarioTraceCollector {
  private val traces = ConcurrentHashMap<String, ScenarioTrace>()
  const val TEST_ID_KEY = "testId"

  fun startTrace(testName: String): String {
    val testId = UUID.randomUUID().toString()
    MDC.put(TEST_ID_KEY, testId)
    traces[testId] = ScenarioTrace(testName)
    return testId
  }

  fun getTrace(): ScenarioTrace? {
    val testId = MDC.get(TEST_ID_KEY) ?: return null
    return traces[testId]
  }

  fun addEvent(event: TraceEvent) {
    getTrace()?.add(event)
  }

  fun logCustom(label: String, data: Any? = null) {
    addEvent(CustomTraceEvent(Instant.now(), label, data))
  }

  fun logDslAction(details: Map<String, Any?> = emptyMap()) {
    addEvent(DslTraceEvent(Instant.now(), DslTraceType.DSL_ACTION, details))
  }

  fun logDslAssert(details: Map<String, Any?> = emptyMap()) {
    addEvent(DslTraceEvent(Instant.now(), DslTraceType.DSL_ASSERT, details))
  }

  fun logDslDbCheck(details: Map<String, Any?> = emptyMap()) {
    addEvent(DslTraceEvent(Instant.now(), DslTraceType.DSL_DB_CHECK, details))
  }

  fun setFixtureSnapshot(snapshot: String) {
    getTrace()?.fixtureSnapshot = snapshot
  }

  fun clear() {
    val testId = MDC.get(TEST_ID_KEY)
    if (testId != null) {
      traces.remove(testId)
      MDC.remove(TEST_ID_KEY)
    }
  }
}
