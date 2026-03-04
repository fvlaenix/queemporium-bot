# AGENTS.md

This guide is for AI coding agents working in `queemporium-bot`. It focuses on how the bot actually works in code and
how to test changes safely.

## Canonical Facts

- Language/runtime: Kotlin 2.x, JVM toolchain 11.
- Entrypoint: `src/main/kotlin/LaunchBot.kt` (`main` in `LaunchBotKt`).
- DI entry modules: `applicationConfigModule`, `botConfigModule`, `coreServiceModule` in
  `src/main/kotlin/com/fvlaenix/queemporium/di/KoinModule.kt`.
- Bot runtime wrapper: `src/main/kotlin/com/fvlaenix/queemporium/DiscordBot.kt`.
- Feature system: `FeatureRegistry`, `FeatureLoader`, `SharedModules`.
- Build: Gradle Kotlin DSL (`build.gradle.kts`).

## Trust Order

When docs conflict, use this order:

1. Source code (`src/main/kotlin`, `src/test/kotlin`)
2. Build files (`build.gradle.kts`, `settings.gradle.kts`)
3. Markdown docs

Do not preserve outdated docs behavior if code differs.

## High-Value Commands

- `./gradlew build`
- `./gradlew test`
- `./gradlew test --tests "com.fvlaenix.queemporium.commands.PingTest"`
- `./gradlew runServer`
- `./gradlew runServerJar`
- `./gradlew clean`
- `./gradlew collectLogs` (packs diagnostics as `build/diagnostics.zip`)

## Project Map

- `src/main/kotlin/com/fvlaenix/queemporium/commands`: Discord command handlers and command-specific configs.
- `src/main/kotlin/com/fvlaenix/queemporium/features`: feature keys, definitions, loader, shared module definitions.
- `src/main/kotlin/com/fvlaenix/queemporium/configuration`: application, DB, duplicate-image, S3 config loaders.
- `src/main/kotlin/com/fvlaenix/queemporium/service`: service contracts and implementations.
- `src/main/kotlin/com/fvlaenix/queemporium/database`: Exposed connectors and persistence tables.
- `src/test/kotlin/com/fvlaenix/queemporium`: test bootstrap, fixture DSL, scenario DSL, virtual time, log enforcement.
- `src/main/resources`: default local config/resources (`application.properties`, yaml/properties samples, logging).
- `discord-bots-rpc`: `.proto` contracts used by Gradle protobuf generation.

## Runtime Architecture

### Boot Sequence

1. `LaunchBot.main()` installs JUL->SLF4J bridge.
2. Starts Koin with base modules (app config, bot config, core services).
3. Loads `BotConfiguration` from YAML.
4. Calls `FeatureLoader.load(botConfiguration)`.
5. Collects all `ListenerAdapter` instances from Koin.
6. Creates `DiscordBot` and starts JDA (`awaitReady`).

### Event Execution Model

- Commands typically extend `CoroutineListenerAdapter`.
- Incoming JDA events are wrapped and dispatched into coroutines on `BotCoroutineProvider.botPool`.
- Per-event MDC context is added (`guildId`, `channelId`, `userId`) for traceable logs.
- Global exception handling is centralized:
    - `EXCEPTION_HANDLER` in coroutine context
    - explicit try/catch in each event override for panic logging
- `receiveMessageFilter(event)` acts as cheap pre-filter before launching command logic.

### JDA Runtime Settings

`DiscordBot` configures:

- Intents: `GUILD_MESSAGES`, `DIRECT_MESSAGES`, `MESSAGE_CONTENT`, plus `GUILD_MEMBERS`.
- Full member chunking/caching.
- Disabled caches: voice state, emoji, sticker, scheduled events.
- Activity: custom status `"Dominates Emporium"`.

## Configuration Truth (Current Code)

### Environment Variables

- Only `APP_CONFIG_PATH` is read directly by production code.
- Legacy vars (`BOT_PROPERTIES`, `DATABASE_PROPERTIES`, `SEARCH_PROPERTIES_PATH`) appear in old docs but are not read
  directly in current source.

### `application.properties`

`ApplicationConfig.load()` resolves:

- `bot.properties.path`
- `database.properties.path`
- `search.properties.path`
- `duplicate.image.properties.path`
- `s3.properties.path`

### Loader Behavior And Failure Modes

- `BotConfiguration.load(applicationConfig)`:
    - Requires `bot.properties.path`.
    - Parses YAML with Kaml.
    - Throws if file is missing or malformed.
- `DatabaseConfiguration.load(applicationConfig)`:
    - Uses path from `database.properties.path` or `/database.properties` resource fallback.
    - Requires `url`, `driver`, `user`, `password`.
    - Fails fast if missing.
- `SearchConfiguration.load(applicationConfig)` (in `SearchCommand.kt`):
    - Uses `search.properties.path` or `/search.properties`.
    - Returns `null` if missing; `SharedModules.searchModule` then throws if `search` feature is enabled without config.
- `DuplicateImageServiceConfig.load(applicationConfig)`:
    - Uses configured path or `/duplicate-image.properties`.
    - Defaults to `hostname=localhost`, `port=50055` if nothing found.
- `S3Configuration.load(applicationConfig)`:
    - Uses configured path or `/s3.properties`.
    - Throws if no file or required property is missing.

## Feature Toggle System

### Where Features Are Declared

- Feature keys and modules: `src/main/kotlin/com/fvlaenix/queemporium/features/FeatureRegistry.kt`.
- Loader behavior: `FeatureLoader`.
- Shared dependencies: `SharedModules`.

### Feature Loading Rules

- Loader iterates through `BotConfiguration.features`.
- Unknown key: warning and skip.
- Disabled key: skip.
- Enabled key:
    - load required shared modules only if missing in Koin
    - load feature-specific modules
- Result: only enabled listeners/services are present in DI.

### Dependency Groups (Important For New Features)

- Core only: `debug`, `ping`, `permissions-info`, `logger-message`
- Core + Search: `search`
- Core + Database:
    - `pixiv-compressed-detector`, `author-collect`, `author-mapping`, `exclude-channel`
    - `messages-store`, `dependent-deleter`, `set-duplicate-channel`
    - `hall-of-fame`, `set-hall-of-fame`, `hall-of-fame-oldest`, `advent`
    - `online-emoji-store`, `long-term-emoji-store`
- Core + Database + Duplicate image service:
    - `upload-pictures`, `revenge-pictures`, `online-picture-compare`
- Core + Database + S3:
    - `send-image`

### Hall Of Fame Behavior (Current Code)

- `set-hall-of-fame` only stores target channel/threshold and shows a histogram; it does not directly enqueue posts.
- `hall-of-fame oldest <max-age>` performs two steps at command time:
  - snapshots current messages above threshold into Hall of Fame backlog storage
  - marks backlog entries as `TO_SEND` only if the original message timestamp is within `<max-age>`
- `<max-age>` currently supports `d`, `w`, and `h` suffixes (examples: `7d`, `1w`, `48h`).
- Backlog age filtering is based on the original message timestamp, not on when threshold crossing was detected.
- Real-time Hall of Fame posting path is triggered by emoji reaction tracking (`OnlineEmojiesStoreCommand` calls
  `HallOfFameCommand.recheckMessage`).

## Testing Deep Dive

### Core Test Bootstrap

- Base class: `BaseKoinTest`.
- `setupBotKoin { ... }` builds a minimal Koin graph with:
    - `TestCoroutineProvider`
    - mocked/stubbed `ApplicationConfig`, `BotConfiguration`, `AnswerService`
    - in-memory DB config by default (`createInMemoryDatabaseConfig()` -> H2 URL)
- `FeatureLoader` runs in tests too, so enabled feature set is realistic.

### Test Extensions Automatically Applied

`BaseKoinTest` uses:

- `ScenarioTestWatcher`: starts per-test trace and writes failure report to `build/reports/scenarios/...`.
- `LogLevelTestExtension`: enforces no unexpected WARN/ERROR logs in package `com.fvlaenix.queemporium`.

### Log Enforcement Rules

- Any unexpected WARN/ERROR log from project package fails the test.
- Expected logs must be declared explicitly:

```kotlin
expectLogs {
  error("com.fvlaenix.queemporium.commands.SearchCommand", count = 1)
  warn("com.fvlaenix.queemporium.commands.AdventCommand", count = 1, messageContains = "No advent configured")
}
```

Useful files:

- `testing/log/LogLevelTestExtension.kt`
- `testing/log/TestLogCapture.kt`
- `testing/log/ExpectedLogs.kt`
- `src/test/resources/logback-test.xml` (writes `build/logs/test-run.log`)

### Fixture DSL (Recommended For Command Behavior)

Main entrypoints:

- `fixture { ... }` in `testing/fixture/FixtureBuilder.kt`
- `setupWithFixture(...)` in `testing/fixture/FixtureIntegration.kt`

What fixture DSL can define:

- enabled features
- users
- guilds/channels
- pre-seeded messages
- pre-seeded reactions
- member admin flags
- message creation time

### Scenario DSL (Recommended For Message/Reaction Flows)

Scenario runner executes ordered steps:

- send message
- add reaction
- advance virtual time
- await jobs
- custom assertions

Main files:

- `testing/scenario/ScenarioBuilder.kt`
- `testing/scenario/ScenarioRunner.kt`

### Virtual Time And Scheduled Jobs

- `VirtualClock` + `VirtualTimeController` allow deterministic time-travel tests.
- `TestCoroutineProvider.safeDelay()` integrates with `VirtualClock`.
- Advancing time resumes pending delays without real waiting.
- `awaitAll()` in fixture integration handles quiescence logic for active jobs.

Use virtual time for features that rely on delays or recurring processing.

### gRPC Integration Tests

- Base: `grpc/BaseGrpcTest.kt`.
- Starts test gRPC server (`TestGrpcServer`) on dynamic port.
- Injects test `DuplicateImageServiceConfig` and real `DuplicateImageServiceImpl`.
- Enables feature set per test via `getFeaturesForTest()`.

### Test Artifacts And Debugging

- Scenario failure reports: `build/reports/scenarios/<TestClass>/<TestName>.report.txt`
- Test logs: `build/logs/test-run.log`
- Aggregated diagnostics archive: `build/diagnostics.zip` (via `collectLogs` task)

## Change Playbook For Agents

### Adding Or Changing A Command Feature

1. Implement/update command class under `commands/`.
2. Register feature key and `FeatureDefinition` in `FeatureRegistry`.
3. Add required shared modules in definition.
4. Ensure listener is bound as `ListenerAdapter`.
5. Add tests:

- happy path
- failure path
- permission/edge conditions
- expected log assertions if warnings/errors are intentional

6. Update docs/examples when keys or params change (`README.md`, `src/main/resources/bot-properties.yaml`).

### Updating Config-Dependent Logic

1. Update corresponding config loader class.
2. Verify fallback and failure behavior.
3. Add tests for missing properties and malformed files where appropriate.
4. Keep error messages specific and actionable.

## Common Pitfalls

- Forgetting to register a feature in `FeatureRegistry` after writing command code.
- Enabling a feature in tests without required shared module dependencies.
- Triggering WARN/ERROR logs without `expectLogs { ... }`.
- Writing tests that rely on real wall-clock delays instead of virtual time.
- Editing generated artifacts under `build/`.
- Accidentally depending on old env var names from historical docs.

## Safety And Hygiene

- Never commit real secrets/tokens/API keys.
- Keep changes scoped; avoid unrelated refactors in the same patch.
- Prefer deterministic tests and no external network calls in test paths.
- Do not hand-edit generated code (protobuf outputs in `build/`).
