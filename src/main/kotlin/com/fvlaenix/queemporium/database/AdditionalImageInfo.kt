package com.fvlaenix.queemporium.database

import kotlinx.serialization.Serializable

@Serializable
data class AdditionalImageInfo(
  val fileName: String,
  val isSpoiler: Boolean, 
  val originalSizeHeight: Int, 
  val originalSizeWidth: Int)

data class Size(val width: Int, val height: Int)

data class CompressSize(val width: Int?, val height: Int?) {
  init {
    assert((height == null && width != null) || (height != null && width == null))
  }
  
  fun getScaledSize(size: Size): Size {
    val scale = if (this.height == null) {
      size.width.toDouble() / this.width!!
    } else if (this.width == null) {
      size.height.toDouble() / this.height
    } else throw IllegalStateException()
    return Size((size.width / scale).toInt(), (size.height / scale).toInt())
  }
}