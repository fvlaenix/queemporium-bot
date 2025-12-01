package com.fvlaenix.queemporium.koin

import com.fvlaenix.queemporium.commands.*
import com.fvlaenix.queemporium.commands.advent.AdventCommand
import com.fvlaenix.queemporium.commands.duplicate.OnlinePictureCompare
import com.fvlaenix.queemporium.commands.duplicate.RevengePicturesCommand
import com.fvlaenix.queemporium.commands.duplicate.UploadPicturesCommand
import com.fvlaenix.queemporium.commands.emoji.LongTermEmojiesStoreCommand
import com.fvlaenix.queemporium.commands.emoji.OnlineEmojiesStoreCommand
import com.fvlaenix.queemporium.commands.halloffame.HallOfFameCommand
import com.fvlaenix.queemporium.commands.halloffame.SetHallOfFameCommand
import com.fvlaenix.queemporium.configuration.ApplicationConfig
import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.configuration.FeatureToggle
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.service.AnswerService
import io.mockk.mockk
import kotlin.reflect.KClass

class BotConfigBuilder {
  var applicationConfig: ApplicationConfig = mockk(relaxed = true)
  var databaseConfig: DatabaseConfiguration = createInMemoryDatabaseConfig()
  var botConfiguration: BotConfiguration = BotConfiguration(token = "test-token", features = emptyMap())
  var answerService: AnswerService = mockk()

  fun enableFeatures(vararg featureKeys: String) {
    botConfiguration = botConfiguration.copy(
      features = featureKeys.associateWith { FeatureToggle(enabled = true) }
    )
  }

  fun enableCommands(vararg commandClasses: KClass<*>) {
    val featureKeys = commandClasses.map { commandClass ->
      when (commandClass) {
        PingCommand::class -> FeatureKeys.PING
        PermissionsInfoCommand::class -> FeatureKeys.PERMISSIONS_INFO
        LoggerMessageCommand::class -> FeatureKeys.LOGGER
        SearchCommand::class -> FeatureKeys.SEARCH
        PixivCompressedDetectorCommand::class -> FeatureKeys.PIXIV_DETECTOR
        AuthorCollectCommand::class -> FeatureKeys.AUTHOR_COLLECT
        AuthorMappingCommand::class -> FeatureKeys.AUTHOR_MAPPING
        ExcludeChannelCommand::class -> FeatureKeys.EXCLUDE_CHANNEL
        MessagesStoreCommand::class -> FeatureKeys.MESSAGES_STORE
        DependentDeleterCommand::class -> FeatureKeys.DEPENDENT_DELETER
        SetDuplicateChannelCommand::class -> FeatureKeys.SET_DUPLICATE_CHANNEL
        UploadPicturesCommand::class -> FeatureKeys.UPLOAD_PICTURES
        RevengePicturesCommand::class -> FeatureKeys.REVENGE_PICTURES
        OnlinePictureCompare::class -> FeatureKeys.ONLINE_COMPARE
        HallOfFameCommand::class -> FeatureKeys.HALL_OF_FAME
        SetHallOfFameCommand::class -> FeatureKeys.SET_HALL_OF_FAME
        AdventCommand::class -> FeatureKeys.ADVENT
        OnlineEmojiesStoreCommand::class -> FeatureKeys.ONLINE_EMOJI
        LongTermEmojiesStoreCommand::class -> FeatureKeys.LONG_TERM_EMOJI
        else -> error("Unknown feature key for command $commandClass")
      }
    }.toTypedArray()
    enableFeatures(*featureKeys)
  }
}
