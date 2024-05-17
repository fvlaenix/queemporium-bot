package com.fvlaenix.queemporium.configuration

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.InputStream
import java.util.*

data class DatabaseConfiguration(
  val url: String,
  val driver: String,
  val user: String,
  val password: String,
) {
  constructor(properties: Properties) : this(
    url = properties.getProperty("url"),
    driver = properties.getProperty("driver"),
    user = properties.getProperty("user"),
    password = properties.getProperty("password")
  )

  constructor(inputStream: InputStream) : this(Properties().apply { load(inputStream) })
  
  fun toDatabase() : Database = Database.connect(
    url = url,
    driver = driver,
    user = user,
    password = password
  ).apply {
    if (!transaction(this) { try { !connection.isClosed } catch (e: Exception) { throw Exception("Test connection failed", e)} }) 
      throw Exception("Test connection failed: closed") 
  }
}