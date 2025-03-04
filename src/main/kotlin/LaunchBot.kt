import com.fvlaenix.queemporium.DiscordBot
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.di.applicationConfigModule
import com.fvlaenix.queemporium.di.configurationModule
import com.fvlaenix.queemporium.di.loadCoreModule
import com.fvlaenix.queemporium.di.productionServiceModule
import org.koin.core.context.GlobalContext.startKoin
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger

const val LOGGING_PATH: String = "/logging.properties"

class LaunchBot

fun main() {
  try {
    LogManager.getLogManager().readConfiguration(LaunchBot::class.java.getResourceAsStream(LOGGING_PATH))
  } catch (e: Exception) {
    throw IllegalStateException("Failed while trying to read logs", e)
  }
  val launchBotLog = Logger.getLogger(LaunchBot::class.java.name)

  launchBotLog.log(Level.INFO, "Starting Koin")
  val koin = startKoin {
    allowOverride(false)
  }.koin

  launchBotLog.log(Level.INFO, "Loading application config module")
  koin.loadModules(listOf(applicationConfigModule))

  launchBotLog.log(Level.INFO, "Loading config module")
  koin.loadModules(listOf(configurationModule))

  launchBotLog.log(Level.INFO, "Loading services module")
  koin.loadModules(listOf(productionServiceModule))

  launchBotLog.log(Level.INFO, "Loading core module")
  koin.loadCoreModule()

  launchBotLog.log(Level.INFO, "Trying to get database configuration")
  val databaseConfiguration = koin.get<DatabaseConfiguration>()

  launchBotLog.log(Level.INFO, "Test database connection")
  databaseConfiguration.toDatabase()

  launchBotLog.log(Level.INFO, "Create bot")
  val bot = koin.get<DiscordBot>()
  bot.run()
}