package com.fvlaenix.queemporium.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
  fun epochToDateString(epoch: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS", Locale.getDefault()).format(Date(epoch))
  }
}