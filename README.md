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

## Custom Lootr Chests
Admins can turn any chest, trapped chest, barrel or shulker box into a Lootr chest and choose which loot table it uses. Look at the container and run:

* `/lootr register <loot_table>` - Register the container as a Lootr chest. It must be empty. Registration is refused if Lootr's compatibility settings would make it ignore the container, for example a shop chest or a chest whose name matches `ignore-chest-names-containing`. The message says which setting caused it.
* `/lootr settable <loot_table>` - Change the loot table of a Lootr chest. Loot already rolled by players is cleared so everyone gets loot from the new table.
* `/lootr unregister` - Turn a Lootr chest back into a normal container.

### Where Lootr looks for loot tables
Lootr does not define or edit loot tables itself. Each Lootr chest stores the name of a loot table (for example `minecraft:chests/simple_dungeon`). The first time a player opens the chest, Lootr asks the server for that table by name and rolls it for that player.

That means a table works with Lootr if the server has it loaded:

* **Vanilla tables:** `minecraft:chests/...`. The `minecraft:` prefix is optional, so `chests/simple_dungeon` also works.
* **Datapack tables:** any table in an enabled datapack. A file at `world/datapacks/my_loot/data/myserver/loot_table/chests/starter_kit.json` becomes `myserver:chests/starter_kit`.
* **Tables from other plugins:** these work too, as long as the plugin registers them with the server as real loot tables.

Tab-completion for `register` and `settable` lists the vanilla chest and pot tables and the tables it finds in the main world's `datapacks` folder. Tables that are loaded some other way still work, but you have to type their full name. Set `loot.suggest-all-vanilla-tables: true` to also list every other vanilla table. Many of them work in a chest:

* **Gameplay tables**, for example `minecraft:gameplay/fishing/junk`, `minecraft:gameplay/cat_morning_gift` or the Hero of the Village gifts.
* **Mob drop tables** (`minecraft:entities/...`). They roll as if the player opening the chest had killed the mob. Some mobs drop nothing in vanilla, so their tables are empty: `minecraft:entities/axolotl` gives an empty chest.

`register` and `settable` test-roll the table once before saving it and refuse tables that need information Lootr can't provide, such as block or interaction tables like `minecraft:shearing/carve_pumpkin` or armadillo brushing. The reason is shown in chat. The test only checks that the table can be rolled at all. It doesn't check whether it gives items, so an empty or very sparse table is accepted.

After adding or changing a datapack table, run `/reload` (or restart) so the server loads it, then `/lootr reload` to refresh tab-completion. If a table fails to load, the server logs an error and `/lootr register` reports it as an unknown loot table.

### Land claims
With `protection.bypass-protection: true` (the default), claim plugins such as GriefPrevention can't lock players out of Lootr chests or minecart chests. Every player gets their own copy of the loot, so claiming land around a Lootr chest doesn't let anyone keep it for themselves. Normal chests, shop chests, and chests in disabled worlds stay protected by the claim as usual. Set it to `false` to let claims block Lootr chests too.

### Loot that changes when a chest is opened
Some plugins don't define loot tables. Instead, they change items while a vanilla chest is being filled, usually by listening for Bukkit's `LootGenerateEvent`. Lootr rolls its per-player inventories itself, so these changes are not guaranteed to show up in Lootr chests. They may appear in normal chests that use the same table but not in Lootr chests. If you use one of these plugins, test it on a single Lootr chest before relying on it.

### Building loot tables
Use a dedicated loot table editor rather than writing the JSON by hand. [Misode's loot table generator](https://misode.github.io/loot-table/) builds tables in the browser; pick your Minecraft version before exporting. Any editor that produces a datapack, or registers real loot tables with the server, works with Lootr.

## Planned Features
These were in earlier versions of `config.yml` but were never implemented, so they have been removed from the default config. Setting them in an existing config has no effect.

* **Loot table allowlist** (`loot.allowed-loot-tables`): only convert chests whose loot table is on a list.
* **Glowing chests** (`visuals.glow-enabled`): highlight Lootr chests with a glow effect.
* **Data backups** (`storage.create-backups`, `storage.max-backups`): keep rotating backups of chest and player data.
* **Translations** (`general.language`): the setting is still in the config, but there is no translation system yet. Player-facing chest messages can be changed in the `messages` section of `config.yml`. Admin command output is English only.

## Version Support
The plugin targets the Paper API for version 26.3 and requires Java 25.

* Maven compiles against `io.papermc.paper:paper-api:26.3.build.214-stable`.
* The codebase only uses stable Bukkit/Paper APIs and no NMS internals, so it should keep working on later 26.x builds as long as Paper keeps those APIs compatible.

### Building
Install JDK 25 and Maven (on macOS: `brew install --cask temurin@25` and `brew install maven`), then run `mvn clean package`. The jar is written to `target/`.

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
