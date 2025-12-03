import com.fvlaenix.queemporium.DiscordBot
import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.di.applicationConfigModule
import com.fvlaenix.queemporium.di.botConfigModule
import com.fvlaenix.queemporium.di.coreServiceModule
import com.fvlaenix.queemporium.features.FeatureLoader
import com.fvlaenix.queemporium.utils.Logging
import org.koin.core.context.GlobalContext.startKoin
import org.slf4j.bridge.SLF4JBridgeHandler

class LaunchBot

fun main() {
  // Bridge java.util.logging to SLF4J
  SLF4JBridgeHandler.removeHandlersForRootLogger()
  SLF4JBridgeHandler.install()

  val launchBotLog = Logging.getLogger(LaunchBot::class.java)

  launchBotLog.info("Starting Koin")
  val koin = startKoin {
    allowOverride(false)
    modules(
      applicationConfigModule,
      botConfigModule,
      coreServiceModule
    )
  }.koin

  launchBotLog.info("Loading feature modules")
  val botConfiguration = koin.get<BotConfiguration>()
  FeatureLoader(koin).load(botConfiguration)

  launchBotLog.info("Create bot")
  val listeners = koin.getAll<net.dv8tion.jda.api.hooks.ListenerAdapter>()
  val bot = DiscordBot(botConfiguration.token, listeners)
  bot.run()
}