# queemporium-bot

[Install](doc/install.md)

Credits:

src/main/resources/images/what-a-pixel.png - Senukin

## Feature toggles

Bot configuration now lives in a YAML file pointed to by `bot.properties.path` from `application.properties` (or
`APP_CONFIG_PATH`). Example:

```yaml
token: "discord-token"
features:
  ping:
    enabled: true
  search:
    enabled: true
  hall-of-fame:
    enabled: false
  online-emoji-store:
    enabled: true
    params:
      distanceInDays: 7
      guildThreshold: 2
```

Feature keys mirror command names: `ping`, `permissions-info`, `logger-message`, `search`, `pixiv-compressed-detector`,
`author-collect`, `author-mapping`, `exclude-channel`, `messages-store`, `dependent-deleter`, `set-duplicate-channel`,
`upload-pictures`, `revenge-pictures`, `online-picture-compare`, `hall-of-fame`, `set-hall-of-fame`, `advent`,
`online-emoji-store`, `long-term-emoji-store`.

Only enabled features load their Koin modules and dependencies (e.g., database or duplicate-image services). Unknown
feature keys are ignored with a warning. Tests can enable a subset of features via
`BotConfigBuilder.enableCommands(...)` or `enableFeatures(...)` to avoid pulling unnecessary dependencies.
