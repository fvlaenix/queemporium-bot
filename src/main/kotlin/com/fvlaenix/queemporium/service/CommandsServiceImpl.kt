package com.fvlaenix.queemporium.service

import com.fvlaenix.queemporium.commands.*
import com.fvlaenix.queemporium.commands.duplicate.OnlinePictureCompare
import com.fvlaenix.queemporium.commands.duplicate.RevengePicturesCommand
import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.configuration.MetadataConfiguration
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.javaType

private val LOG = Logger.getLogger(CommandsServiceImpl::class.java.name)

class CommandsServiceImpl(
  private val metadataConfiguration: MetadataConfiguration
) : CommandsService {
  @OptIn(ExperimentalStdlibApi::class)
  override fun getCommands(
    botConfiguration: BotConfiguration,
    databaseConfiguration: DatabaseConfiguration,
    answerService: AnswerServiceImpl
  ): List<ListenerAdapter> {
    val defaultFeatures = STANDARD_COMMANDS.map {
      Feature(it, true, emptyMap())
    }
    val features = botConfiguration.features.map { feature ->
      val clazz = try {
        Class.forName(feature.className).kotlin
      } catch (e: ClassNotFoundException) { throw Exception("Can't find class ${feature.className} in bot config. Please make sure you wrote it correctly") }
      Feature(clazz, feature.enable, feature.parameter.associate { it.name to it.value })
    }
    val allFeatures = metadataConfiguration.commands.map { command ->
      try {
        val clazz = Class.forName(command.className).kotlin
        var superClass: Class<*>? = clazz.java.superclass
        while (superClass != ListenerAdapter::class.java) {
          check(superClass != null) { "Class $clazz should inherit interface ${ListenerAdapter::class}" }
          superClass = superClass.superclass
        }
        clazz
      } catch (e: ClassNotFoundException) {
        throw Exception("Can't find class ${command.className} in metadata. Please make sure you wrote it correctly")
      }
    }

    val resultFeatures = mutableListOf<Feature>()

    fun addFeature(feature: Feature) {
      if (!allFeatures.contains(feature.clazz)) {
        throw Exception("Trying to add feature not from list of features: ${feature.clazz}")
      }
      resultFeatures.removeAll { it.clazz == feature.clazz }
      if (feature.enabled) {
        resultFeatures.add(feature)
      }
    }

    defaultFeatures.forEach(::addFeature)
    features.forEach(::addFeature)

    // convert into listeners

    val listeners = resultFeatures.map { feature ->
      val constructors = feature.clazz.constructors
      check(constructors.size == 1) { "${feature.clazz} should have only one constructor" }
      val constructor = constructors.first()
      val parameters: List<Any> = constructor.parameters.map { kParameter ->
        when (kParameter.type.javaType) {
          DatabaseConfiguration::class.java -> databaseConfiguration
          AnswerService::class.java -> answerService
          Map::class.java -> feature.parameters
          else -> throw IllegalArgumentException("Unsupported parameter type: ${kParameter.type}")
        }
      }
      constructor.call(*parameters.toTypedArray()) as ListenerAdapter
    }
    LOG.log(Level.INFO, "Bot options: ${listeners.map { it::class.simpleName }}")
    return listeners
  }

  companion object {
    private val STANDARD_COMMANDS: List<KClass<*>> = listOf(
      OnlinePictureCompare::class,
      RevengePicturesCommand::class,
      AuthorCollectCommand::class,
      AuthorMappingCommand::class,
      DependentDeleterCommand::class,
      EmojiesStoreCommand::class,
      ExcludeChannelCommand::class,
      LoggerMessageCommand::class,
      MessagesStoreCommand::class,
      PixivCompressedDetectorCommand::class,
      SetDuplicateChannelCommand::class,
      SearchCommand::class
    )
  }

  data class Feature(
    val clazz: KClass<*>,
    val enabled: Boolean,
    val parameters: Map<String, String>
  )
}