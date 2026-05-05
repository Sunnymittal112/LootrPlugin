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

## Patch Update - May 5, 2026

### Feature: Claim Protection Bypass
This update introduces a major improvement to how Lootr interacts with land-claim and region-protection plugins such as GriefPrevention, WorldGuard, Lands, and Towny.

#### Problem Solved
Previously, if a Lootr chest was located inside a protected claim or region (such as a server spawn, a player's house, or a protected dungeon), protection plugins would cancel the interaction, preventing players from opening the chest even though Lootr is designed to be accessible to everyone.

#### Solution Implemented
* Dynamic Event Un-cancellation: The plugin now monitors for cancelled interactions at the highest priority. If it detects a player trying to open a Lootr chest in a protected area, it will override the cancellation.
* Configurable Bypass: A new setting 'protection.bypass-protection' has been added to config.yml. When set to true, it enables this bypass logic.
* Intelligent Filtering: The bypass logic automatically ignores shop chests (QuickShop, etc.) and containers with specific metadata or custom names to ensure that only legitimate Lootr chests are bypassed.
* Performance Optimization: The chest verification logic has been refactored into efficient helper methods to ensure that high-traffic servers do not experience lag during interaction checks.

#### Configuration Addition
Add or update the following in your config.yml:
```yaml
protection:
  bypass-protection: true
```

#### Technical Note
This update ensures that the "Always Accessible" philosophy of Lootr is maintained across all claim plugins while still respecting "Break Protections"—meaning players can open the chests in claims, but they still cannot destroy them.
