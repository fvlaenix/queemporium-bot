package com.fvlaenix.queemporium.configuration

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class BotConfiguration(
  @XmlSerialName("token")
  val token: Token,
  @XmlSerialName("feature")
  val features: List<Feature> = emptyList(),
) {
  @Serializable
  data class Feature(
    val className: String,
    @XmlSerialName("enable")
    val enable: Boolean = true,
    @XmlSerialName("parameter")
    val parameter: List<Parameter> = emptyList()
  ) {
    @Serializable
    data class Parameter(
      val name: String,
      val value: String
    )
  }

  @Serializable
  data class Token(
    val raw: String
  )
}