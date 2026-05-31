<p align="center">
  <img src="docs/assets/lumoaidetector-hero.svg" alt="LumoAiDetector">
</p>

<p align="center">
  <a href="https://github.com/isLumo/LumoAiDetector/releases"><img alt="Release" src="https://img.shields.io/badge/version-0.0.1-2f81f7?style=for-the-badge"></a>
  <a href="https://github.com/isLumo/LumoAiDetector/blob/main/LICENSE"><img alt="License" src="https://img.shields.io/badge/license-Apache%202.0-2f81f7?style=for-the-badge"></a>
  <img alt="Java" src="https://img.shields.io/badge/java-8+-2f81f7?style=for-the-badge">
  <img alt="Gradle" src="https://img.shields.io/badge/build-Gradle-2f81f7?style=for-the-badge">
  <img alt="Minecraft" src="https://img.shields.io/badge/minecraft-1.8%20to%20modern-2f81f7?style=for-the-badge">
</p>

<p align="center">
  <a href="#install">Install</a>
  ·
  <a href="#commands">Commands</a>
  ·
  <a href="#training-your-own-model">Training</a>
  ·
  <a href="#configuration">Config</a>
  ·
  <a href="#license-and-forks">License</a>
</p>

# LumoAiDetector

LumoAiDetector is a free Minecraft anti-cheat experiment built around local AI model training. It records combat rotation windows, filters out bad data, trains a Smile Random Forest model inside Java, then alerts staff when a player starts looking suspicious.

I first made this for my own server. After working on it for a while, I decided to publish it instead of keeping it private. On 01.06.2026, I could find free anti-cheats and a few ML ideas, but I could not find a free plugin that matched this exact workflow: record your own dataset, train your own local model, manage dated model files in game, and keep every message configurable.

The plugin is free because I want server owners to be able to test this idea without paying for a closed model or running a separate Python service. I am not publishing my own trained model. Train yours. The better your dataset is, the better the detector will be.

<p align="center">
  <img src="docs/assets/lumoaidetector-pipeline.svg" alt="How LumoAiDetector learns">
</p>

## What it does

LumoAiDetector watches combat rotations, not random movement. A window reaches the model only when all gates pass:

- The player recently attacked or swung their hand.
- A living target is nearby and inside the player's aim area.
- The mouse moved enough across the 15 tick window.
- Tick timing stayed inside the configured network range.

Each valid window becomes 120 numeric features:

```text
15 ticks * 8 values = dx, dy, dt, v, a, j, err, derr
```

The class label is simple:

```text
0 = legit
1 = cheater
```

<p align="center">
  <img src="docs/assets/lumoaidetector-comparison.svg" alt="LumoAiDetector comparison">
</p>

## Why train your own model

Different servers have different players, clients, ping patterns, arenas, mobs, PvP styles, and cheat settings. A model trained on one small private setup can look impressive in a demo and still be useless on your server.

Train on boring normal fights. Train on cracked aim settings. Train on smooth aim settings. Train on bad players, good players, high sensitivity, low sensitivity, different DPI, different weapons, jumping, strafing, panic flicks, and missed hits. The model gets smarter when the dataset stops being too clean.

If you only record one legit player and one cheat profile, you are teaching the model a tiny story. Give it many stories.

## Features

- Gradle project ready for IntelliJ IDEA.
- Bukkit, Spigot, Paper, Purpur and Folia friendly structure.
- Java 8 bytecode target.
- No NMS and no ProtocolLib requirement.
- Configurable `plugins/LumoAiDetector/config.yml`.
- Configurable `plugins/LumoAiDetector/messages.yml`.
- Admin command `/lad`.
- Dataset recording for legit and cheater samples.
- CSV window format with 15 ticks and 120 features.
- Anti garbage gates for combat, target, movement and ping.
- Async model training with Smile Random Forest.
- Dated model files with metadata.
- Model activation and deactivation in game.
- Temporary model backups after delete.
- Staff alerts above the configured suspicion percent.
- Permission based tab complete.
- Auto punishment support is present, but disabled by default.

## Install

1. Build the jar.
2. Put `build/libs/LumoAiDetector-0.0.1.jar` into your server `plugins` folder.
3. Start the server once.
4. Edit `plugins/LumoAiDetector/config.yml` only if you know what you want to tune.
5. Edit `plugins/LumoAiDetector/messages.yml` if you want different text.

## Build in IntelliJ IDEA

Open the folder as a Gradle project:

```text
File -> Open -> LumoAiDetector
```

Then run:

```text
Gradle -> Tasks -> shadow -> shadowJar
```

The plugin jar will be here:

```text
build/libs/LumoAiDetector-0.0.1.jar
```

Console build:

```powershell
gradle clean shadowJar
```

## Commands

```text
/lad reload
/lad status
/lad record legit <player>
/lad record cheater <player>
/lad record info [all|legit|cheater] [page]
/lad train
/lad active <model>
/lad deactive
/lad check <player>
/lad models [page]
/lad deleted <model>
/lad backup list [page]
/lad backup restore <backup>
/lad backup purge <backup>
```

## Permissions

```text
LumoAiDetector.admin
LumoAiDetector.reload
LumoAiDetector.status
LumoAiDetector.record
LumoAiDetector.train
LumoAiDetector.active
LumoAiDetector.deactive
LumoAiDetector.check
LumoAiDetector.models
LumoAiDetector.delete
LumoAiDetector.backup
LumoAiDetector.alert
```

## Training your own model

Start with a local test server. Spawn a zombie, use a bot, or fight another account.

Record legit data:

```text
/lad record legit <yourName>
```

Play normally. Move around. Jump. Flick. Track targets smoothly. Miss sometimes. Change sensitivity and DPI during the session. Record several types of normal play, not only your best aim.

Record cheater data:

```text
/lad record cheater <yourName>
```

Test different cheat profiles. Fast aim, slow smoothing, obvious Killaura, subtle rotations, weird settings, and anything you expect real cheaters to use.

Train:

```text
/lad train
```

Activate:

```text
/lad active <model>
```

The plugin names models by date and time. Metadata is saved next to every model, so you can see training time, dataset size and basic metrics.

## Configuration

The default config is conservative. Alerts are enabled. Auto punishment is disabled. This is intentional.

Do not enable punishments until you have trained and tested your own model. A model is only as good as its data.

Main files:

```text
plugins/LumoAiDetector/config.yml
plugins/LumoAiDetector/messages.yml
plugins/LumoAiDetector/data/dataset.csv
plugins/LumoAiDetector/models/
plugins/LumoAiDetector/backups/models/
plugins/LumoAiDetector/stats.yml
plugins/LumoAiDetector/runtime.yml
```

## License and forks

LumoAiDetector is licensed under Apache License 2.0.

Copyright 2026 Lumo (Lumskyy).

The license lets people study, use, modify and share the code. It also requires them to keep the license and attribution notices. That matters to me. I am fine with forks, fixes, experiments and ports. I am not fine with someone removing my name, reuploading the plugin as if they wrote the original, or using `LumoAiDetector`, `Lumo`, or `Lumskyy` branding to make an unofficial build look official.

That is why the project includes a `NOTICE` file. If someone breaks the license or uses the name in a misleading way, I may ask GitHub, plugin marketplaces, hosting platforms, or other relevant services to remove the copy or correct the attribution. I may also use the options allowed by the Apache License 2.0 and platform rules.

Unofficial forks should use a clearly different name and say that they are based on LumoAiDetector by Lumo (Lumskyy).

## Current status

Version `0.0.1` is the first public source release. Test it on a local server before using it on production. Keep backups of models and datasets. If something breaks, open an issue with server version, Java version, plugin version, logs, config changes and what command or combat action caused the problem.

This project is free. I want it to stay useful, understandable and honest.
