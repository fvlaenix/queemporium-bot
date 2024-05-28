package com.fvlaenix.queemporium.database

import kotlinx.serialization.Serializable

@Serializable
data class MessageProblems(val imagesProblems: List<ImageProblem>) {
  @Serializable
  data class ImageProblem(val imageNumber: Int)
}