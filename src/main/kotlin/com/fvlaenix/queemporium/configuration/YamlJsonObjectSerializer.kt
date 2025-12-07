package com.fvlaenix.queemporium.configuration

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Custom serializer that allows deserializing params from YAML into JsonObject.
 * This works around the limitation that JsonObject can only be deserialized from JSON format.
 * Instead, we deserialize from YAML to a generic map and convert primitive values to JsonPrimitive.
 */
object YamlJsonObjectSerializer : KSerializer<JsonObject> {
  // Delegate to a map serializer that handles YAML scalar values as strings
  private val delegateSerializer = MapSerializer(String.serializer(), String.serializer())

  override val descriptor: SerialDescriptor = delegateSerializer.descriptor

  override fun deserialize(decoder: Decoder): JsonObject {
    // First, decode as Map<String, String> - YAML will convert all values to strings
    val stringMap = try {
      decoder.decodeSerializableValue(delegateSerializer)
    } catch (e: Exception) {
      // If params is missing or empty, return empty JsonObject
      return JsonObject(emptyMap())
    }

    // Convert string values to appropriate JsonPrimitives
    val jsonMap = stringMap.mapValues { (_, value) ->
      // Try to parse as different primitive types
      value.toIntOrNull()?.let { return@mapValues JsonPrimitive(it) }
      value.toLongOrNull()?.let { return@mapValues JsonPrimitive(it) }
      value.toDoubleOrNull()?.let { return@mapValues JsonPrimitive(it) }
      value.toBooleanStrictOrNull()?.let { return@mapValues JsonPrimitive(it) }
      // Default to string
      JsonPrimitive(value)
    }

    return JsonObject(jsonMap)
  }

  override fun serialize(encoder: Encoder, value: JsonObject) {
    // For serialization, convert JsonPrimitives back to strings
    val stringMap: Map<String, String> = value.mapValues { (_, jsonElement) ->
      (jsonElement as? JsonPrimitive)?.content ?: jsonElement.toString()
    }
    encoder.encodeSerializableValue(delegateSerializer, stringMap)
  }
}
