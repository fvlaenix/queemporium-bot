package com.fvlaenix.queemporium.features

import com.fvlaenix.queemporium.commands.*
import com.fvlaenix.queemporium.commands.advent.AdventCommand
import com.fvlaenix.queemporium.commands.duplicate.OnlinePictureCompare
import com.fvlaenix.queemporium.commands.duplicate.RevengePicturesCommand
import com.fvlaenix.queemporium.commands.duplicate.UploadPicturesCommand
import com.fvlaenix.queemporium.commands.emoji.LongTermEmojiesStoreCommand
import com.fvlaenix.queemporium.commands.emoji.OnlineEmojiesStoreCommand
import com.fvlaenix.queemporium.commands.halloffame.HallOfFameCommand
import com.fvlaenix.queemporium.commands.halloffame.SetHallOfFameCommand
import com.fvlaenix.queemporium.configuration.commands.LongTermEmojiesStoreCommandConfig
import com.fvlaenix.queemporium.configuration.commands.OnlineEmojiesStoreCommandConfig
import org.koin.dsl.bind
import org.koin.dsl.module

object FeatureKeys {
  const val DEBUG = "debug"
  const val PING = "ping"
  const val PERMISSIONS_INFO = "permissions-info"
  const val LOGGER = "logger-message"
  const val SEARCH = "search"
  const val PIXIV_DETECTOR = "pixiv-compressed-detector"
  const val AUTHOR_COLLECT = "author-collect"
  const val AUTHOR_MAPPING = "author-mapping"
  const val EXCLUDE_CHANNEL = "exclude-channel"
  const val MESSAGES_STORE = "messages-store"
  const val DEPENDENT_DELETER = "dependent-deleter"
  const val SET_DUPLICATE_CHANNEL = "set-duplicate-channel"
  const val UPLOAD_PICTURES = "upload-pictures"
  const val REVENGE_PICTURES = "revenge-pictures"
  const val ONLINE_COMPARE = "online-picture-compare"
  const val HALL_OF_FAME = "hall-of-fame"
  const val SET_HALL_OF_FAME = "set-hall-of-fame"
  const val ADVENT = "advent"
  const val ONLINE_EMOJI = "online-emoji-store"
  const val LONG_TERM_EMOJI = "long-term-emoji-store"
}

object FeatureRegistry {
  val definitions: Map<String, FeatureDefinition> = listOf(
    FeatureDefinition(
      key = FeatureKeys.DEBUG,
      requiredSharedModules = listOf(SharedModules.coreModule)
    ) { _ ->
      listOf(
        module {
          single { DebugCommand(get()) } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.PING,
      requiredSharedModules = listOf(SharedModules.coreModule)
    ) { _ ->
      listOf(
        module {
          single { PingCommand(get(), get()) } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.PERMISSIONS_INFO,
      requiredSharedModules = listOf(SharedModules.coreModule)
    ) { _ ->
      listOf(
        module {
          single { PermissionsInfoCommand(get(), get()) } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.LOGGER,
      requiredSharedModules = listOf(SharedModules.coreModule)
    ) { _ ->
      listOf(
        module {
          single { LoggerMessageCommand(get()) } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.SEARCH,
      requiredSharedModules = listOf(SharedModules.coreModule, SharedModules.searchModule)
    ) { _ ->
      listOf(
        module {
          single { SearchCommand(get(), get(), get()) } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.PIXIV_DETECTOR,
      requiredSharedModules = listOf(SharedModules.coreModule, SharedModules.databaseModule)
    ) { _ ->
      listOf(
        module {
          single {
            PixivCompressedDetectorCommand(
              get(),
              get(),
              get()
            )
          } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.AUTHOR_COLLECT,
      requiredSharedModules = listOf(SharedModules.coreModule, SharedModules.databaseModule)
    ) { _ ->
      listOf(
        module {
          single { AuthorCollectCommand(get(), get()) } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.AUTHOR_MAPPING,
      requiredSharedModules = listOf(SharedModules.coreModule, SharedModules.databaseModule)
    ) { _ ->
      listOf(
        module {
          single { AuthorMappingCommand(get(), get(), get()) } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.EXCLUDE_CHANNEL,
      requiredSharedModules = listOf(SharedModules.coreModule, SharedModules.databaseModule)
    ) { _ ->
      listOf(
        module {
          single { ExcludeChannelCommand(get(), get(), get()) } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.MESSAGES_STORE,
      requiredSharedModules = listOf(SharedModules.coreModule, SharedModules.databaseModule)
    ) { _ ->
      listOf(
        module {
          single { MessagesStoreCommand(get(), get()) } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.DEPENDENT_DELETER,
      requiredSharedModules = listOf(SharedModules.coreModule, SharedModules.databaseModule)
    ) { _ ->
      listOf(
        module {
          single { DependentDeleterCommand(get(), get()) } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.SET_DUPLICATE_CHANNEL,
      requiredSharedModules = listOf(SharedModules.coreModule, SharedModules.databaseModule)
    ) { _ ->
      listOf(
        module {
          single {
            SetDuplicateChannelCommand(
              get(),
              get(),
              get()
            )
          } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.UPLOAD_PICTURES,
      requiredSharedModules = listOf(
        SharedModules.coreModule,
        SharedModules.databaseModule,
        SharedModules.duplicateImageModule
      )
    ) { _ ->
      listOf(
        module {
          single {
            UploadPicturesCommand(
              get(),
              get(),
              get(),
              get()
            )
          } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.REVENGE_PICTURES,
      requiredSharedModules = listOf(
        SharedModules.coreModule,
        SharedModules.databaseModule,
        SharedModules.duplicateImageModule
      )
    ) { _ ->
      listOf(
        module {
          single {
            RevengePicturesCommand(
              get(),
              get(),
              get(),
              get()
            )
          } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.ONLINE_COMPARE,
      requiredSharedModules = listOf(
        SharedModules.coreModule,
        SharedModules.databaseModule,
        SharedModules.duplicateImageModule
      )
    ) { _ ->
      listOf(
        module {
          single {
            OnlinePictureCompare(
              get(),
              get(),
              get(),
              get()
            )
          } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.HALL_OF_FAME,
      requiredSharedModules = listOf(SharedModules.coreModule, SharedModules.databaseModule)
    ) { _ ->
      listOf(
        module {
          single { HallOfFameCommand(get(), get(), get(), get()) } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.SET_HALL_OF_FAME,
      requiredSharedModules = listOf(SharedModules.coreModule, SharedModules.databaseModule)
    ) { _ ->
      listOf(
        module {
          single { SetHallOfFameCommand(get(), get(), get()) } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.ADVENT,
      requiredSharedModules = listOf(SharedModules.coreModule, SharedModules.databaseModule)
    ) { _ ->
      listOf(
        module {
          single { AdventCommand(get(), get(), get(), get()) } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.ONLINE_EMOJI,
      requiredSharedModules = listOf(SharedModules.coreModule, SharedModules.databaseModule)
    ) { toggle ->
      val config = OnlineEmojiesStoreCommandConfig.fromParams(toggle.params)
      listOf(
        module {
          single { config }
          single {
            OnlineEmojiesStoreCommand(
              get(),
              get(),
              get()
            )
          } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    },
    FeatureDefinition(
      key = FeatureKeys.LONG_TERM_EMOJI,
      requiredSharedModules = listOf(SharedModules.coreModule, SharedModules.databaseModule)
    ) { toggle ->
      val config = LongTermEmojiesStoreCommandConfig.fromParams(toggle.params)
      listOf(
        module {
          single { config }
          single {
            LongTermEmojiesStoreCommand(
              get(),
              get(),
              get()
            )
          } bind net.dv8tion.jda.api.hooks.ListenerAdapter::class
        }
      )
    }
  ).associateBy { it.key }
}
