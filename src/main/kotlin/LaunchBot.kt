import com.fvlaenix.queemporium.DiscordBot
import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import java.io.InputStream
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger
import kotlin.io.path.Path
import kotlin.io.path.inputStream

const val LOGGING_PATH: String = "/logging.properties"

val BOT_PROPERTIES_PATH_STRING: String? = System.getenv("BOT_PROPERTIES")
val DATABASE_PROPERTIES_PATH_STRING: String? = System.getenv("DATABASE_PROPERTIES")

val BOT_PROPERTIES_INPUT_STREAM: InputStream = 
  BOT_PROPERTIES_PATH_STRING?.let { try { Path(it).inputStream() } catch (e: Exception) { throw Exception("Can't open file", e) } } ?:
  LaunchBot::class.java.getResourceAsStream("bot.properties") ?:
  throw IllegalStateException("Cannot find bot properties in standard files")

val DATABASE_PROPERTIES_INPUT_STREAM: InputStream =
  DATABASE_PROPERTIES_PATH_STRING?.let { try { Path(it).inputStream() } catch (e: Exception) { throw Exception("Can't open file", e) } } ?:
  LaunchBot::class.java.getResourceAsStream("database.properties") ?:
  throw IllegalStateException("Cannot find database properties in standard files")

class LaunchBot

fun main() {
  try {
    LogManager.getLogManager().readConfiguration(LaunchBot::class.java.getResourceAsStream(LOGGING_PATH))
  } catch (e: Exception) {
    throw IllegalStateException("Failed while trying to read logs", e)
  }
  val launchBotLog = Logger.getLogger(LaunchBot::class.java.name)
  launchBotLog.log(Level.INFO, "Trying to get bot configuration")
  val botConfiguration = BotConfiguration(BOT_PROPERTIES_INPUT_STREAM)
  launchBotLog.log(Level.INFO, "Trying to get database configuration")
  val databaseConfiguration = DatabaseConfiguration(DATABASE_PROPERTIES_INPUT_STREAM)
  launchBotLog.log(Level.INFO, "Test database connection")
  databaseConfiguration.toDatabase()
  launchBotLog.log(Level.INFO, "Bot and database configurations loaded successfully")
  val bot = DiscordBot(botConfiguration, databaseConfiguration)
  bot.run()
}