# Lootr Plugin Documentation

## Overview
Lootr is a specialized Minecraft server plugin that revolutionizes how loot is distributed in your world. In standard Minecraft, when a player finds a dungeon chest, they take the items, and the chest becomes empty for everyone else. Lootr solves this problem by creating "instanced" loot.

When a player opens a Lootr chest, the plugin generates a unique inventory specifically for that player. Other players can open the same physical chest and will see their own unique set of items. This ensures that exploration remains rewarding for all players on a multiplayer server, regardless of who arrived at a structure first.

## Key Functionalities
* Unique Loot for Every Player: Every player gets their own loot from every chest.
* Automatic Conversion: The plugin automatically detects vanilla chests with loot tables (like those in dungeons, desert temples, and bastions) and converts them into Lootr chests.
* Inventory Persistence: If a player leaves items in a Lootr chest, those items will be saved and waiting for them when they return.
* Comprehensive Protection: Lootr chests are protected from being broken, moved by pistons, or looted by hoppers to prevent griefing and automation of rare loot.
* Visual Indicators: Includes a particle system to highlight chests that a player hasn't looted yet, making exploration more intuitive.

## Installation Guide
1. Download the LootrPlugin.jar file.
2. Place the jar file into your server's 'plugins' folder.
3. Restart your server to generate the configuration files.
4. (Optional) Customize the settings in 'plugins/LootrPlugin/config.yml'.
5. Use '/lootr reload' to apply any changes made to the configuration.

## Version Support
The plugin now targets the Paper API for version 26.1.2.

What was changed:
* Maven now compiles against `io.papermc.paper:paper-api:26.1.2.build.60-stable`.
* `plugin.yml` now declares `api-version: '26.1.2'`.

Compatibility note:
* This codebase only uses stable Bukkit/Paper APIs and no NMS internals, so it is a strong candidate for running on later 26.x server builds.
* Actual compatibility for future builds such as 26.2.x or 26.3.x still depends on Paper keeping those APIs binary-compatible.
* The repository has been updated and validated against the currently published stable 26.1.2 Paper API.

## Patch Update - July 26, 2026

### What changed in this patch
This update expands Lootr’s container support and improves compatibility with newer server APIs.

#### New container support
* Lootr now supports chests, barrels, and minecart chests.
* Opening these containers now uses the same per-player loot flow and protection logic as regular Lootr chests.

#### Compatibility improvements
* Added a compatibility layer for newer Paper/Bukkit API behavior.
* Updated inventory creation and sound playback handling for better compatibility across modern server versions.
* Improved loot-context handling so loot tables are generated more reliably.
* Modernized admin command handling to avoid older deprecated lookup patterns.

#### Build and manifest updates
* The plugin package was verified successfully after these changes.
* The project is now better aligned with modern Paper compatibility expectations while preserving existing functionality.
