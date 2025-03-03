package com.fvlaenix.queemporium.koin

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import java.util.*

fun createInMemoryDatabaseConfig(additionalInfo: String? = null): DatabaseConfiguration {
  val dbId = UUID.randomUUID().toString()
  return DatabaseConfiguration(
    url = "jdbc:h2:mem:test_db_$dbId${if (additionalInfo != null) "_$additionalInfo" else ""};DB_CLOSE_DELAY=-1",
    driver = "org.h2.Driver",
    user = "sa",
    password = ""
  )
}