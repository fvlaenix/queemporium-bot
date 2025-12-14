package com.fvlaenix.queemporium.mock

import com.fvlaenix.queemporium.service.S3FileResult
import com.fvlaenix.queemporium.service.S3FileService

class MockS3FileService : S3FileService {
  private val responsesByS3Path = mutableMapOf<String, S3FileResult>()
  var defaultResponse: S3FileResult = S3FileResult.NotFound

  fun setResponseForPath(s3Path: String, response: S3FileResult) {
    responsesByS3Path[s3Path] = response
  }

  fun clearResponses() {
    responsesByS3Path.clear()
  }

  override suspend fun fetchFile(s3Path: String): S3FileResult {
    return responsesByS3Path[s3Path] ?: defaultResponse
  }
}
