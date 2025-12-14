package com.fvlaenix.queemporium.service

data class S3FileData(
  val bytes: ByteArray,
  val filename: String
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as S3FileData

    if (!bytes.contentEquals(other.bytes)) return false
    if (filename != other.filename) return false

    return true
  }

  override fun hashCode(): Int {
    var result = bytes.contentHashCode()
    result = 31 * result + filename.hashCode()
    return result
  }
}

sealed class S3FileResult {
  data class Success(val data: S3FileData) : S3FileResult()
  data object NotFound : S3FileResult()
  data object TooLarge : S3FileResult()
  data class Error(val message: String) : S3FileResult()
}

interface S3FileService {
  suspend fun fetchFile(s3Path: String): S3FileResult
}
