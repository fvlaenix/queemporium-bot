package com.fvlaenix.queemporium.service

import com.fvlaenix.queemporium.commands.*
import com.fvlaenix.queemporium.commands.advent.AdventCommand
import com.fvlaenix.queemporium.commands.duplicate.OnlinePictureCompare
import com.fvlaenix.queemporium.commands.duplicate.RevengePicturesCommand
import com.fvlaenix.queemporium.commands.emoji.OnlineEmojiesStoreCommand
import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.MetadataConfiguration
import net.dv8tion.jda.api.hooks.ListenerAdapter
import nl.adaptivity.xmlutil.core.impl.multiplatform.name
import org.koin.core.Koin
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.TypeQualifier
import org.koin.dsl.module
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.memberFunctions

private val LOG = Logger.getLogger(CommandsServiceImpl::class.java.name)

class CommandsServiceImpl(
  private val koin: Koin,
  private val metadataConfiguration: MetadataConfiguration
) : CommandsService {
  @OptIn(ExperimentalStdlibApi::class)
  override fun getCommands(
    botConfiguration: BotConfiguration
  ): List<ListenerAdapter> {
    val defaultFeatures = STANDARD_COMMANDS.map {
      Feature(it, true, emptyMap())
    }
    val features = botConfiguration.features.map { feature ->
      val clazz = try {
        Class.forName(feature.className).kotlin
      } catch (_: ClassNotFoundException) {
        throw Exception("Can't find class ${feature.className} in bot config. Please make sure you wrote it correctly")
      }
      Feature(clazz, feature.enable, feature.parameter.associate { it.name to it.value })
    }
    val allFeatures = metadataConfiguration.commands.map { command ->
      try {
        val clazz = Class.forName(command.className).kotlin
        validateCommandClass(clazz)
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
    data class InitializationClassInfo(
      val clazz: KClass<*>,
      val reason: List<KClass<*>>
    )

    val classesToInitialization: MutableList<InitializationClassInfo> = resultFeatures
      .map { (clazz, _, _) -> InitializationClassInfo(clazz, listOf(clazz)) }
      .toMutableList()
    val listeners: MutableList<ListenerAdapter> = mutableListOf()

    while (classesToInitialization.isNotEmpty()) {
      val (clazz, reason) = classesToInitialization.removeFirst()
      if (clazz.isAbstract) {
        throw Exception(
          "Class ${clazz.name} is abstract and can't be initialized! Make sure you add correct mapping with abstract and non abstract classes!\n" +
              "Reason of calling:\n${reason.joinToString("\n") { it.name }}"
        )
      }
      val constructor = try {
        getConstructorFunction(clazz)
      } catch (e: Exception) {
        throw Exception(
          "Failed to find constructor for ${clazz.name}!\n" +
              "Reason of calling:\n${reason.joinToString("\n") { it.name }}",
          e
        )
      }
      val uninitializedParameters = mutableListOf<KClass<*>>()
      val parameters = constructor.parameters.map { param ->
        val classifier = (param.type.classifier as KClass<*>).let {
          if (SERVICE_MAPPING.containsKey(it)) {
            SERVICE_MAPPING[it]!!
          } else {
            it
          }
        }
        if (classifier.javaPrimitiveType != null) {
          throw Exception(
            "Can't inject primitive type ${classifier.simpleName} in class ${clazz.simpleName}. Found primitive type in constructor of class.\n" +
                "Reason of calling:\n${reason.joinToString("\n") { it.name }}"
          )
        }
        val parameter = if (classifier.isCompanion) {
          classifier.objectInstance
        } else {
          koin.getOrNull<Any>(classifier) ?: koin.getOrNull<Any>(TypeQualifier(classifier))
        }
        if (parameter == null) {
          uninitializedParameters.add(classifier)
        }
        parameter
      }

      if (uninitializedParameters.isNotEmpty()) {
        classesToInitialization.add(0, InitializationClassInfo(clazz, reason))
        classesToInitialization.addAll(
          0,
          uninitializedParameters.map { InitializationClassInfo(it, listOf(it) + reason) })
      } else {
        val instance =
          constructor.call(*parameters.toTypedArray()) ?: throw Exception(
            "Constructor should return non-null value, but for some reason it returned null. Check logs for more information.\n" +
                "Reason of calling:\n${reason.joinToString("\n") { it.name }}"
          )
        koin.declare(instance, TypeQualifier(clazz))
        if (instance is ListenerAdapter) {
          listeners.add(instance)
        }
      }
    }

    val module = module {
      listeners.forEach { listener ->
        single { listener }
      }
    }
    loadKoinModules(module)

    LOG.log(Level.INFO, "Bot options: ${listeners.map { it::class.simpleName }}")
    return listeners
  }

  private fun validateCommandClass(commandClass: KClass<*>) {
    var superClass: Class<*>? = commandClass.java.superclass
    while (superClass != ListenerAdapter::class.java) {
      check(superClass != null) { "Class $commandClass should inherit interface ${ListenerAdapter::class}" }
      superClass = superClass.superclass
    }
  }

  private fun getConstructorFunction(clazz: KClass<*>): KFunction<*> {
    val companionObject = clazz.companionObject
    if (companionObject != null) {
      val loadFunction = companionObject.memberFunctions
        .filter { function -> function.name == "load" }
      if (loadFunction.size == 1) {
        return loadFunction.first()
      }
      check(loadFunction.isEmpty()) { "Class ${clazz.name} shouldn't have several load functions! I can't select one!" }
    }
    val constructors = clazz.constructors
    check(constructors.size == 1) { "$clazz should have only one constructor! I can't select one! Count: ${constructors.size}" }
    return constructors.first()
  }

  companion object {
    private val STANDARD_COMMANDS: List<KClass<*>> = listOf(
      OnlinePictureCompare::class,
      RevengePicturesCommand::class,
      AuthorCollectCommand::class,
      AuthorMappingCommand::class,
      DependentDeleterCommand::class,
      OnlineEmojiesStoreCommand::class,
      ExcludeChannelCommand::class,
      LoggerMessageCommand::class,
      MessagesStoreCommand::class,
      PixivCompressedDetectorCommand::class,
      SetDuplicateChannelCommand::class,
      SearchCommand::class,
      AdventCommand::class,
      PermissionsInfoCommand::class
    )

    private val SERVICE_MAPPING: Map<KClass<*>, KClass<*>> = mapOf(
      AnswerService::class to AnswerServiceImpl::class,
      DuplicateImageService::class to DuplicateImageServiceImpl::class,
      SearchService::class to SearchServiceImpl::class,
    )
  }

  data class Feature(
    val clazz: KClass<*>,
    val enabled: Boolean,
    val parameters: Map<String, String>
  )
}