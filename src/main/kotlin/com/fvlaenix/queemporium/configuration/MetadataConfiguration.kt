package com.fvlaenix.queemporium.configuration

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class MetadataConfiguration(
  @XmlSerialName("command")
  val commands: List<Command>
) {
  @Serializable
  data class Command(val className: String)
}