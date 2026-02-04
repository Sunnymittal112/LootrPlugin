# Lootr

**Per-player loot chests for Paper servers. No more loot stealing!**

Inspired by the [Lootr Mod](https://modrinth.com/mod/lootr). Every player gets their own unique loot from structure chests.

## Features

- 🎁 **Per-player loot** - Same chest, different loot for each player
- 🏛️ **Structure support** - Dungeons, temples, mansions, and more
- 📦 **Datapack compatible** - Works with custom structure datapacks
- 🔒 **Protected** - Anti-break, anti-hopper, anti-explosion
- ⚡ **1.18 - 1.21** - Full Paper compatibility

## How It Works

1. Find a chest in any structure
2. Open it → Get unique loot
3. Other players open same chest → Get their own loot
4. For you, it becomes a normal chest after first open

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/lootr reload` | `lootr.admin` | Reload config |
| `/lootr convert` | `lootr.admin` | Convert nearby chests |
| `/lootr custom` | `lootr.admin` | Create custom chest |
| `/lootr debug` | `lootr.admin` | Debug loot tables |

## Config

```yaml
settings:
  auto-convert: true
  particles: true
  
loot:
  refresh-hours: 0  # 0 = one time per player
  
protection:
  prevent-break: true
  prevent-hopper: true
