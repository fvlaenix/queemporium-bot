import com.fvlaenix.queemporium.DiscordBot
import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.configuration.MetadataConfiguration
import com.fvlaenix.queemporium.service.AnswerServiceImpl
import com.fvlaenix.queemporium.service.CommandsServiceImpl
import com.fvlaenix.queemporium.utils.XmlUtils.XML
import java.io.InputStream
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger
import kotlin.io.path.Path
import kotlin.io.path.inputStream

const val LOGGING_PATH: String = "/logging.properties"
const val METADATA_PATH: String = "/metadata.xml"

val BOT_PROPERTIES_PATH_STRING: String? = System.getenv("BOT_PROPERTIES")
val DATABASE_PROPERTIES_PATH_STRING: String? = System.getenv("DATABASE_PROPERTIES")

val METADATA_INPUT_STREAM: InputStream =
  LaunchBot::class.java.getResourceAsStream(METADATA_PATH) ?: throw IllegalStateException("Cannot find metadata in standard files")

val BOT_PROPERTIES_INPUT_STREAM: InputStream =
  BOT_PROPERTIES_PATH_STRING?.let { try { Path(it).inputStream() } catch (e: Exception) { throw Exception("Can't open file", e) } } ?:
  LaunchBot::class.java.getResourceAsStream("bot-properties.xml") ?:
  throw IllegalStateException("Cannot find bot properties in standard files")

val DATABASE_PROPERTIES_INPUT_STREAM: InputStream =
  DATABASE_PROPERTIES_PATH_STRING?.let { try { Path(it).inputStream() } catch (e: Exception) { throw Exception("Can't open file", e) } } ?:
  LaunchBot::class.java.getResourceAsStream("database.properties") ?:
  throw IllegalStateException("Cannot find database properties in standard files")

val DUPLICATE_IMAGE_HOSTNAME = System.getenv("DUPLICATE_IMAGE_HOSTNAME") ?: "localhost"

class LaunchBot

fun main() {
  try {
    LogManager.getLogManager().readConfiguration(LaunchBot::class.java.getResourceAsStream(LOGGING_PATH))
  } catch (e: Exception) {
    throw IllegalStateException("Failed while trying to read logs", e)
  }
  val launchBotLog = Logger.getLogger(LaunchBot::class.java.name)

  launchBotLog.log(Level.INFO, "Trying to get metadata")
  val metadataConfiguration = XML.decodeFromString<MetadataConfiguration>(METADATA_INPUT_STREAM.reader().readText())

  launchBotLog.log(Level.INFO, "Trying to get bot configuration")
  val botConfiguration = XML.decodeFromString<BotConfiguration>(BOT_PROPERTIES_INPUT_STREAM.reader().readText())

  launchBotLog.log(Level.INFO, "Trying to get database configuration")
  val databaseConfiguration = DatabaseConfiguration(DATABASE_PROPERTIES_INPUT_STREAM)

  launchBotLog.log(Level.INFO, "Test database connection")
  databaseConfiguration.toDatabase()

  launchBotLog.log(Level.INFO, "Create commands")
  val commandService = CommandsServiceImpl(metadataConfiguration)

  launchBotLog.log(Level.INFO, "Create answer service")
  val answerService = AnswerServiceImpl()

  launchBotLog.log(Level.INFO, "Create bot")
  val bot = DiscordBot(botConfiguration, databaseConfiguration, commandService, answerService)
  bot.run()
}