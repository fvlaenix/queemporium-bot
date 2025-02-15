# Queemporium Discord Bot

## Project Overview
Queemporium is a feature-rich Discord bot built with JDA (Java Discord API) that provides various functionalities for server management and image processing.

## Technical Stack
### Core Technologies
- **Language**: Kotlin with Coroutines support
- **Discord API**: JDA (Java Discord API)
- **Database**: MySQL with Exposed ORM
- **Dependency Injection**: Koin
- **Communication**: gRPC
- **Image Processing**: TwelveMonkeys ImageIO
- **Image Search**: SauceNAO API integration

### Key Dependencies
- JDA 5.2.0 for Discord integration
- Exposed 0.17.14 for database operations
- Koin 4.0.1 for dependency injection
- Kotlinx Coroutines for asynchronous operations
- TwelveMonkeys ImageIO for extensive image format support
- SauceNAO API for image searching capabilities

## Architecture
### Modular Design
The project follows a modular architecture with clear separation of concerns:
1. **Configuration Modules**
   - Application configuration
   - Bot configuration
   - Database configuration
2. **Core Module**
   - Command handling
   - Event listening
   - Service implementations

### Key Components
1. **Bot Core (DiscordBot.kt)**
   - Manages bot initialization and lifecycle
   - Handles Discord gateway intents
   - Configures caching policies
   - Manages thread pools for operations

2. **Command System**
   - Implements a service-based command pattern
   - Handles message events and command processing
   - Supports extensible command structure

3. **Database Integration**
   - MySQL database connection
   - ORM mapping with Exposed
   - Guild information management

4. **Image Processing**
   - Support for multiple image formats
   - Image manipulation capabilities
   - Integration with SauceNAO for image searching

## Development Guidelines
### Threading
- Uses dedicated thread pools for bot operations
- Main bot pool with 4 threads
- Coroutine scope for asynchronous operations

### Configuration
- Modular configuration system
- Environment-based settings
- Separate configurations for bot, database, and application

### Logging
- Comprehensive logging system
- Custom logging properties
- Different log levels for various operations

## Getting Started
1. Configure the required environment variables
2. Set up the MySQL database
3. Run the bot using the provided Gradle tasks:
   ```bash
   ./gradlew runServer
   ```
   or build a JAR:
   ```bash
   ./gradlew runServerJar
   ```

## Building and Deployment
The project uses Gradle with the following key tasks:
- `runServer`: Runs the bot directly
- `runServerJar`: Creates an executable JAR
- `test`: Runs the test suite

## Implementing New Commands
### Command Structure
1. **Base Class**
   - Extend `CoroutineListenerAdapter` for coroutine support
   - Override necessary event handlers (e.g., `onMessageReceivedSuspend`)
   - Implement message filtering with `receiveMessageFilter`

2. **Dependency Injection**
   - Use constructor injection for services and configurations
   - Services are automatically mapped in CommandsServiceImpl
   - Available configurations:
      - DatabaseConfiguration
      - BotConfiguration
      - ApplicationConfig
      - MetadataConfiguration

3. **Database Integration**
   - Use DatabaseConfiguration for database connection
   - Create data models and connectors
   - Example:
     ```kotlin
     class YourCommand(val databaseConfiguration: DatabaseConfiguration) : CoroutineListenerAdapter() {
         private val connector = YourConnector(databaseConfiguration.toDatabase())
         // ... command implementation
     }
     ```

4. **Asynchronous Operations**
   - Use suspend functions for async operations
   - Access shared thread pool via `DiscordBot.MAIN_BOT_POOL`
   - Use `CoroutineScope` from `DiscordBot.MAIN_SCOPE`

5. **Error Handling**
   - Global exception handler is provided
   - Log errors appropriately using the Logger
   - Handle Discord API exceptions gracefully

### Command Registration
1. **Standard Commands**
   - Add your command to `STANDARD_COMMANDS` in CommandsServiceImpl
   - Example:
     ```kotlin
     private val STANDARD_COMMANDS: List<KClass<*>> = listOf(
         YourCommand::class,
         // other commands...
     )
     ```

2. **Service Mappings**
   - Register service implementations in `SERVICE_MAPPING`
   - Used for interface-implementation mappings
   - Example:
     ```kotlin
     private val SERVICE_MAPPING: Map<KClass<*>, KClass<*>> = mapOf(
         YourService::class to YourServiceImpl::class,
     )
     ```

3. **Metadata Configuration**
   - Configure commands through metadata.xml
   - Uses kotlinx.serialization for XML parsing
   - Supports command class registration
   - Can be loaded from custom path via applicationConfig.metadataPropertiesPath
   - Default location is /metadata.xml in resources
   - Example:
     ```xml
     <?xml version="1.0" encoding="UTF-8"?>
     <MetadataConfiguration>
         <command>
             <className>com.fvlaenix.queemporium.commands.YourCommand</className>