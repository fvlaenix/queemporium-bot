# Implementing Complex Commands in Queemporium Bot

## Table of Contents
1. [Overview](#overview)
2. [Command Structure](#command-structure)
3. [Service Integration](#service-integration)
4. [Database Integration](#database-integration)
5. [Configuration Management](#configuration-management)
6. [Error Handling](#error-handling)
7. [Examples](#examples)

## Overview
This guide provides comprehensive instructions for implementing complex commands in the Queemporium Discord bot. Complex commands typically involve multiple services, database interactions, and sophisticated business logic.

## Command Structure

### Base Setup
```kotlin
class YourComplexCommand(
    private val databaseConfiguration: DatabaseConfiguration,
    private val yourService: YourService,
    private val answerService: AnswerService
) : CoroutineListenerAdapter() {
    // Database connectors initialization
    private val yourConnector = YourConnector(databaseConfiguration.toDatabase())

    // Thread pool for heavy operations
    @OptIn(DelicateCoroutinesApi::class)
    private val customContext = newFixedThreadPoolContext(4, "Your Operation Pool")
}
```

### Event Handling
```kotlin
override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    if (!event.isFromGuild) return
    val message = event.message

    // Guild-specific checks
    if (yourConnector.isChannelExclude(message.guildId!!, message.channelId)) return

    coroutineScope {
        launch(customContext + EXCEPTION_HANDLER) {
            // Your complex logic here
            processMessage(message)
        }
    }
}
```

## Service Integration

### Service Design
Services should be designed to handle specific business logic:

```kotlin
interface YourService {
    suspend fun processData(data: YourData): ProcessingResult
    suspend fun validateInput(input: UserInput): Boolean
    suspend fun handleResponse(result: ProcessingResult): List<String>
}

class YourServiceImpl(
    private val config: YourConfiguration,
    private val otherService: OtherService
) : YourService {
    override suspend fun processData(data: YourData): ProcessingResult {
        // Implementation
    }
}
```

### Service Registration
Register your service in CommandsServiceImpl:
```kotlin
private val SERVICE_MAPPING: Map<KClass<*>, KClass<*>> = mapOf(
    YourService::class to YourServiceImpl::class
)
```

## Database Integration

### Connector Pattern
```kotlin
class YourConnector(private val database: Database) {
    object YourTable : Table() {
        val id = varchar("id", 255)
        val data = text("data")
        override val primaryKey = PrimaryKey(id)
    }

    suspend fun getData(id: String): YourData? = dbQuery {
        YourTable.select { YourTable.id eq id }
            .map { it[YourTable.data] }
            .singleOrNull()
    }
}
```

### Transaction Management
```kotlin
private suspend fun <T> dbQuery(block: () -> T): T =
    withContext(Dispatchers.IO) {
        transaction(database) {
            block()
        }
    }
```

## Configuration Management

### Configuration Classes
```kotlin
data class YourConfiguration(
    val apiKey: String,
    val endpoint: String
) {
    companion object {
        fun load(applicationConfig: ApplicationConfig): YourConfiguration? {
            val properties = loadProperties(applicationConfig.yourPropertiesPath)
            return YourConfiguration(
                apiKey = properties.getProperty("API_KEY"),
                endpoint = properties.getProperty("ENDPOINT")
            )
        }
    }
}
```

### Environment Configuration
```kotlin
private fun loadProperties(path: String?): Properties {
    val properties = Properties()
    val stream = path?.let { Path(it).inputStream() }
        ?: YourCommand::class.java.getResourceAsStream("/your.properties")
        ?: throw IllegalStateException("Configuration file not found")
    properties.load(stream)
    return properties
}
```

## Error Handling

### Global Exception Handler
```kotlin
val EXCEPTION_HANDLER = CoroutineExceptionHandler { _, exception ->
    LOG.log(Level.SEVERE, "Error in command execution", exception)
}
```

### Command-Level Error Handling
```kotlin
override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    try {
        // Your logic here
    } catch (e: Exception) {
        LOG.log(Level.SEVERE, "Error processing message", e)
        event.message.reply("An error occurred: ${e.message}").queue()
    }
}
```

## External API Integration

### Resource Management
```kotlin
class ExternalApiService(
    private val config: ServiceConfiguration
) {
    suspend fun callExternalApi(request: Request): Response {
        val api = ExternalAPI(config.apiKey)
        return try {
            api.makeRequest(request)
        } finally {
            api.close()
        }
    }
}
```

### Message Size Management
```kotlin
fun formatDiscordMessages(content: List<String>, maxSize: Int = 1500): List<String> {
    val messages = mutableListOf<String>()
    var currentMessage = content.firstOrNull() ?: return emptyList()

    for (i in 1 until content.size) {
        val nextPart = content[i]
        if ((currentMessage + "\n$nextPart").length > maxSize) {
            messages += currentMessage
            currentMessage = nextPart
        } else {
            currentMessage += "\n$nextPart"
        }
    }

    if (currentMessage.isNotEmpty()) {
        messages += currentMessage
    }

    return messages.map { it.trim() }
}
```

## Examples

### Image Processing Command
```kotlin
class ImageProcessingCommand(
    private val databaseConfiguration: DatabaseConfiguration,
    private val imageService: ImageService,
    private val answerService: AnswerService
) : CoroutineListenerAdapter() {
    private val imageDataConnector = ImageDataConnector(databaseConfiguration.toDatabase())

    override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
        val message = event.message
        val attachments = message.attachments.filter { it.isImage }

        coroutineScope {
            launch(Dispatchers.IO + EXCEPTION_HANDLER) {
                for (attachment in attachments) {
                    val result = imageService.processImage(attachment.url)
                    imageDataConnector.saveResult(message.id, result)
                    answerService.sendProcessingResult(message.channel, result)
                }
            }
        }
    }
}
```

### Database Integration Example
```kotlin
class ComplexDataCommand(
    private val databaseConfiguration: DatabaseConfiguration,
    private val processingService: ProcessingService
) : CoroutineListenerAdapter() {
    private val dataConnector = DataConnector(databaseConfiguration.toDatabase())

    override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
        val message = event.message

        coroutineScope {
            launch(Dispatchers.IO + EXCEPTION_HANDLER) {
                val existingData = dataConnector.getData(message.id)
                if (existingData != null) {
                    val processedData = processingService.process(existingData)
                    dataConnector.updateData(message.id, processedData)
                    message.reply("Data processed successfully").queue()
                }
            }
        }
    }
}
```

### Service Integration Example
```kotlin
interface ComplexService {
    suspend fun processRequest(request: Request): Response
}

class ComplexServiceImpl(
    private val config: ServiceConfiguration,
    private val otherService: OtherService
) : ComplexService {
    override suspend fun processRequest(request: Request): Response = coroutineScope {
        val validationJob = async { validateRequest(request) }
        val preprocessJob = async { preprocessRequest(request) }

        if (!validationJob.await()) {
            throw IllegalArgumentException("Invalid request")
        }

        val preprocessedData = preprocessJob.await()
        val result = otherService.process(preprocessedData)

        Response(result)
    }
}
```

## Best Practices

1. **Command Organization**
   - Keep commands focused on a single responsibility
   - Use services for complex business logic
   - Implement proper error handling

2. **Service Design**
   - Design services to be reusable
   - Use dependency injection
   - Implement proper interfaces

3. **Database Operations**
   - Use connectors for database access
   - Implement proper transaction management
   - Handle database errors gracefully

4. **Configuration Management**
   - Use proper configuration classes
   - Support both default and custom configurations
   - Validate configuration values

5. **Error Handling**
   - Implement proper exception handling
   - Use logging effectively
   - Provide meaningful error messages

6. **Performance Considerations**
   - Use appropriate thread pools
   - Implement proper coroutine scopes
   - Handle resource cleanup

7. **Testing**
   - Write unit tests for commands
   - Test edge cases
   - Mock external dependencies

Remember to always consider:
- Thread safety
- Resource management
- Error handling
- Performance implications
- Code maintainability
