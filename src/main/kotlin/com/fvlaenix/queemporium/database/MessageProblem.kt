package com.fvlaenix.queemporium.database

import kotlinx.serialization.Serializable

@Serializable
sealed class MessageProblem {
  @Serializable
  sealed class ImageProblem : MessageProblem() {
    abstract val imageNumber: Int
    
    @Serializable
    class InternalError(private val myImageNumber: Int, val error: String) : ImageProblem() {
      override val imageNumber: Int
        get() = myImageNumber
    }
  }
}