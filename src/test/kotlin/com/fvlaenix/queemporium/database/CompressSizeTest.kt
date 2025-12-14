package com.fvlaenix.queemporium.database

import kotlin.test.Test
import kotlin.test.assertFailsWith

class CompressSizeTest {

  @Test
  fun `fails when both dimensions are null`() {
    assertFailsWith<IllegalArgumentException> {
      CompressSize(width = null, height = null)
    }
  }

  @Test
  fun `fails when both dimensions are set`() {
    assertFailsWith<IllegalArgumentException> {
      CompressSize(width = 100, height = 200)
    }
  }
}
