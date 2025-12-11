package com.fvlaenix.queemporium.testing.trace

import java.io.File
import java.io.PrintWriter
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ScenarioReportWriter {
  private val timestampFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    .withZone(ZoneId.systemDefault())

  fun writeReport(
    testName: String,
    exception: Throwable?,
    trace: ScenarioTrace,
    outputDir: File
  ) {
    val reportFile = File(outputDir, "${testName}.report.txt")
    reportFile.parentFile.mkdirs()

    reportFile.printWriter().use { writer ->
      writeHeader(writer, testName)
      writeStackTrace(writer, exception)
      writeTrace(writer, trace)
    }

    println("\n--- Scenario Failure Report ---")
    println("Report saved to: ${reportFile.absolutePath}")
    printSummary(trace, exception)
    println("-------------------------------\n")
  }

  private fun writeHeader(writer: PrintWriter, testName: String) {
    writer.println("Test: $testName")
    writer.println("Date: ${java.time.Instant.now()}")
    writer.println("=" * 80)
    writer.println()
  }

  private fun writeStackTrace(writer: PrintWriter, exception: Throwable?) {
    if (exception != null) {
      writer.println("Exception:")
      exception.printStackTrace(writer)
      writer.println()
      writer.println("=" * 80)
      writer.println()
    }
  }

  private fun writeTrace(writer: PrintWriter, trace: ScenarioTrace) {
    writer.println("Scenario Trace:")
    if (trace.fixtureSnapshot.isNotEmpty()) {
      writer.println("Fixture Snapshot:")
      writer.println(trace.fixtureSnapshot)
      writer.println("-" * 40)
    }

    trace.events.forEach { event ->
      val time = timestampFormatter.format(event.timestamp)
      when (event) {
        is ScenarioStepEvent -> {
          writer.println("[$time] STEP: ${event.type}")
          event.details.forEach { (k, v) -> writer.println("  $k: $v") }
        }
        is DslTraceEvent -> {
          writer.println("[$time] ${event.type.name}:")
          event.details.forEach { (k, v) -> writer.println("  $k: $v") }
        }

        is TimeAdvanceEvent -> {
          writer.println("[$time] TIME: Advanced by ${event.duration} -> ${event.newTime}")
        }

        is BotMessageEvent -> {
          writer.println("[$time] BOT: Message to ${event.channelId}")
          writer.println("  Text: ${event.text}")
          if (event.images.isNotEmpty()) {
            writer.println("  Images: ${event.images.map { it.fileName }}")
          }
          if (event.forwardFrom != null) {
            writer.println("  Forwarded from: ${event.forwardFrom}")
          }
        }

        is CustomTraceEvent -> {
          writer.println("[$time] LOG: ${event.label} - ${event.data}")
        }
      }
    }
  }

  private fun printSummary(trace: ScenarioTrace, exception: Throwable?) {
    // Print last few events to console
    val lastEvents = trace.events.takeLast(10)
    if (lastEvents.isNotEmpty()) {
      println("Last ${lastEvents.size} trace events:")
      lastEvents.forEach { event ->
        val time = timestampFormatter.format(event.timestamp)
        val desc = when (event) {
          is ScenarioStepEvent -> "STEP ${event.type}"
          is DslTraceEvent -> event.type.name
          is TimeAdvanceEvent -> "TIME +${event.duration}"
          is BotMessageEvent -> "BOT ${event.text.take(50)}"
          is CustomTraceEvent -> "LOG ${event.label}"
        }
        println("  [$time] $desc")
      }
    }
    if (exception != null) {
      println("Error: ${exception.message}")
    }
  }
}

private operator fun String.times(n: Int): String = this.repeat(n)
