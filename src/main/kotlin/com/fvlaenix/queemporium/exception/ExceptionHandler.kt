package com.fvlaenix.queemporium.exception

import kotlinx.coroutines.CoroutineExceptionHandler
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class ExceptionHandler

private val LOG: Logger = Logger.getLogger(ExceptionHandler::class.java.name)

val EXCEPTION_HANDLER = CoroutineExceptionHandler { _, exception ->
  LOG.log(Level.SEVERE, "Exception while coroutine run", exception)
}