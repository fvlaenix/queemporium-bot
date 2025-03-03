package com.fvlaenix.queemporium.koin

import com.fvlaenix.queemporium.configuration.ApplicationConfig
import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.configuration.MetadataConfiguration
import com.fvlaenix.queemporium.service.AnswerService
import io.mockk.every
import io.mockk.mockk
import kotlin.reflect.KClass

class BotConfigBuilder {
    var applicationConfig: ApplicationConfig = mockk()
    var databaseConfig: DatabaseConfiguration = createInMemoryDatabaseConfig()
    var botConfiguration: BotConfiguration = mockk()
    var metadataConfiguration: MetadataConfiguration = mockk()
    var answerService: AnswerService = mockk()

    fun enableCommands(vararg commandClassNames: String) {
        val commands = commandClassNames.map { MetadataConfiguration.Command(it) }

        val features = commandClassNames.map { 
            BotConfiguration.Feature(it, true, emptyList())
        }

        every { metadataConfiguration.commands } returns commands
        every { botConfiguration.features } returns features
    }

    inline fun <reified T : Any> enableCommands(vararg commandClasses: KClass<T>) {
        enableCommands(*commandClasses.map { it.java.name }.toTypedArray())
    }
}