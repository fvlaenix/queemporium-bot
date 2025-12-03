package com.fvlaenix.queemporium.testing.trace

import com.fvlaenix.queemporium.service.AnswerService.ImageUploadInfo
import java.time.Instant
import kotlin.time.Duration

sealed class TraceEvent {
  abstract val timestamp: Instant
}

data class ScenarioStepEvent(
  override val timestamp: Instant,
  val type: String,
  val details: Map<String, Any?>
) : TraceEvent()

data class TimeAdvanceEvent(
  override val timestamp: Instant,
  val duration: Duration,
  val newTime: Instant
) : TraceEvent()

data class BotMessageEvent(
  override val timestamp: Instant,
  val channelId: String,
  val text: String,
  val images: List<ImageUploadInfo>,
  val forwardFrom: String? = null
) : TraceEvent()

data class CustomTraceEvent(
  override val timestamp: Instant,
  val label: String,
  val data: Any?
) : TraceEvent()

data class ScenarioTrace(
  val testName: String,
  val events: MutableList<TraceEvent> = mutableListOf(),
  var fixtureSnapshot: String = ""
) {
  fun add(event: TraceEvent) {
    events.add(event)
  }
}
