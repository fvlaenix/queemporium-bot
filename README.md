# queemporium-bot

`queemporium-bot` is a modular Discord bot written in Kotlin. It uses feature toggles to load only the commands and
dependencies you enable.

## Stack

- Kotlin 2.0 (JVM toolchain 11)
- JDA 5
- Koin 4 for dependency injection
- gRPC/Protobuf for service integrations
- Exposed + MySQL for persistence
- JUnit 5 + MockK for tests

## How It Starts

1. `LaunchBotKt` starts Koin with base modules.
2. `BotConfiguration` is loaded from YAML.
3. `FeatureLoader` loads enabled feature modules from `FeatureRegistry`.
4. All enabled `ListenerAdapter`s are registered in JDA.

## Build And Run

- `./gradlew build` - compile, run tests, and build artifacts.
- `./gradlew test` - run the test suite.
- `./gradlew runServer` - run from source (`LaunchBotKt`).
- `./gradlew runServerJar` - build `build/libs/runServer.jar`.
- `./gradlew clean` - remove build outputs.

## Configuration

### 1) Master config location

- `APP_CONFIG_PATH` (optional): path to `application.properties`.
- If `APP_CONFIG_PATH` is not set, the app loads `/application.properties` from resources.

### 2) `application.properties`

This file points to feature-specific configs:

```properties
bot.properties.path=/path/to/bot-properties.yaml
database.properties.path=/path/to/database.properties
search.properties.path=/path/to/search.properties
duplicate.image.properties.path=/path/to/duplicate-image.properties
s3.properties.path=/path/to/s3.properties
```

Only `bot.properties.path` is strictly required at startup. Others are required only when their feature modules are
enabled.

### 3) Bot feature YAML

Minimal example:

```yaml
token: "YOUR_DISCORD_BOT_TOKEN"
features:
  ping:
    enabled: true
  search:
    enabled: false
  hall-of-fame:
    enabled: true
    params:
      debug: false
```

Unknown feature keys are ignored with a warning.

## Feature Keys

Current keys from `FeatureRegistry`:

- `debug`
- `ping`
- `permissions-info`
- `logger-message`
- `search`
- `pixiv-compressed-detector`
- `author-collect`
- `author-mapping`
- `exclude-channel`
- `messages-store`
- `dependent-deleter`
- `set-duplicate-channel`
- `upload-pictures`
- `revenge-pictures`
- `online-picture-compare`
- `hall-of-fame`
- `set-hall-of-fame`
- `hall-of-fame-oldest`
- `advent`
- `online-emoji-store`
- `long-term-emoji-store`
- `send-image`

Reference sample config in repository: `src/main/resources/bot-properties.yaml` (replace secrets with your own values).

## Project Layout

- `src/main/kotlin` - bot commands, DI, config, feature loading, services.
- `src/main/resources` - default config/resources and logging config.
- `src/test/kotlin` - tests, fixtures, scenario DSL, virtual-time utilities.
- `discord-bots-rpc` - protobuf contracts for generated gRPC stubs.
- `docs` and `doc` - testing/command guides and deployment notes.

## Testing Notes

- Test base (`BaseKoinTest`) auto-enforces no unexpected WARN/ERROR logs from project packages.
- For expected warnings/errors, tests should declare them with `expectLogs { ... }`.
- Use fixture DSL and virtual-time helpers for deterministic command tests (
  `src/test/kotlin/com/fvlaenix/queemporium/testing`).

## Documentation

- Install and deployment notes: [doc/install.md](doc/install.md)
- Complex command guide: [docs/complex_command_guide.md](docs/complex_command_guide.md)

## Security

- Do not commit live tokens, API keys, or credentials.
- Prefer external config files and secret management in CI/production.

## Credits

- `src/main/resources/images/what-a-pixel.jpg` - Senukin
