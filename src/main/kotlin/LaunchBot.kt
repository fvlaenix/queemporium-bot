import com.fvlaenix.queemporium.DiscordBot
import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.di.applicationConfigModule
import com.fvlaenix.queemporium.di.botConfigModule
import com.fvlaenix.queemporium.di.coreServiceModule
import com.fvlaenix.queemporium.features.FeatureLoader
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
    modules(
      applicationConfigModule,
      botConfigModule,
      coreServiceModule
    )
  }.koin

  launchBotLog.log(Level.INFO, "Loading feature modules")
  val botConfiguration = koin.get<BotConfiguration>()
  FeatureLoader(koin).load(botConfiguration)

  launchBotLog.log(Level.INFO, "Create bot")
  val listeners = koin.getAll<net.dv8tion.jda.api.hooks.ListenerAdapter>()
  val bot = DiscordBot(botConfiguration.token, listeners)
  bot.run()
}
