package com.fvlaenix.queemporium.utils

import com.fvlaenix.queemporium.database.MessageProblem
import com.fvlaenix.queemporium.exception.EXCEPTION_HANDLER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

object CoroutineUtils {
  class CurrentMessageMessageProblemHandler(val messageProblems: MutableList<MessageProblem> = mutableListOf()) : CoroutineContext.Element {
    override val key = CURRENT_MESSAGE_EXCEPTION_CONTEXT_KEY
  }
  val CURRENT_MESSAGE_EXCEPTION_CONTEXT_KEY = object : CoroutineContext.Key<CurrentMessageMessageProblemHandler> {}
  
  suspend fun <INPUT, OUTPUT> CoroutineScope.channelTransform(inputChannel: Channel<INPUT>, threshold: Int, convert: suspend (INPUT) -> OUTPUT?): Channel<OUTPUT> {
    val jobs = mutableListOf<Job>()
    val semaphore = Semaphore(threshold)
    val outputChannel = Channel<OUTPUT>(Channel.UNLIMITED)
    
    launch(EXCEPTION_HANDLER) {
      for (element in inputChannel) {
        semaphore.withPermit {
          val result = convert(element) ?: return@withPermit
          outputChannel.send(result)
        }
      }
      launch(EXCEPTION_HANDLER) {
        jobs.joinAll()
        outputChannel.close()
      }
    }
    return outputChannel
  }
  
  suspend fun <INPUT, OUTPUT> CoroutineScope.flatChannelTransform(inputChannel: Channel<INPUT>, threshold: Int, convert: suspend (INPUT) -> List<OUTPUT>): Channel<OUTPUT> {
    val jobs = mutableListOf<Job>()
    val semaphore = Semaphore(threshold)
    val outputChannel = Channel<OUTPUT>(Channel.UNLIMITED)

    launch(EXCEPTION_HANDLER) {
      for (element in inputChannel) {
        val job = launch(EXCEPTION_HANDLER) {
          semaphore.withPermit {
            val result = convert(element)
            result.forEach { outputChannel.send(it) }
          } 
        }
        jobs.add(job)
      }
      launch(EXCEPTION_HANDLER) {
        jobs.joinAll()
        outputChannel.close()
      }
    }
    return outputChannel
  }
  
  suspend fun <INPUT, OUTPUT> CoroutineScope.channelTransform(input: Collection<INPUT>, threshold: Int, convert: suspend (INPUT) -> OUTPUT?): Channel<OUTPUT> {
    val inputChannel = Channel<INPUT>(Channel.UNLIMITED)
    for (element in input) {
      inputChannel.send(element)
    }
    inputChannel.close()
    return channelTransform(inputChannel, threshold, convert)
  }

  suspend fun <INPUT, OUTPUT> CoroutineScope.flatChannelTransform(input: Collection<INPUT>, threshold: Int, convert: suspend (INPUT) -> List<OUTPUT>): Channel<OUTPUT> {
    val inputChannel = Channel<INPUT>(Channel.UNLIMITED)
    for (element in input) {
      inputChannel.send(element)
    }
    inputChannel.close()
    return flatChannelTransform(inputChannel, threshold, convert)
  }
  
  data class AtomicProgressCounter(
    val done: AtomicInteger = AtomicInteger(0),
    val total: AtomicInteger = AtomicInteger(0)
  ) {
    fun doneIncrement() = done.getAndIncrement()
    fun totalIncrement() = total.getAndIncrement()
    fun totalIncrease(count: Int) = total.getAndAdd(count)
    
    fun status() = "[${done.get()}/${total.get()}]"
  }
}