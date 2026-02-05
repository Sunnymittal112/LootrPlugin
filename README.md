# Lootr

**Per-player loot chests for Paper servers. No more loot stealing!**

Inspired by the [Lootr Mod](https://modrinth.com/mod/lootr). Every player gets their own unique loot from structure chests.

## Features

- 🎁 **Per-player loot** - Same chest, different loot for each player
- 🏛️ **Structure support** - Dungeons, temples, mansions, and more
- 📦 **Datapack compatible** - Works with custom structure datapacks
- 🔒 **Protected** - Anti-break, anti-hopper, anti-explosion
- 🛡️ **Gamemode protection** - Survival/Adventure players cannot break chests
- ⚡ **OP Double-break** - OP/Admins must break twice to confirm destruction
- ⚡ **1.18 - 1.21** - Full Paper compatibility

## How It Works

1. Find a chest in any structure
2. Open it → Get unique loot
3. Other players open same chest → Get their own loot
4. For you, it becomes a normal chest after first open
5. Chest stays protected so other players can still use it

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/lootr` | `lootr.admin` | Main admin command |
| `/lootr reload` | `lootr.admin` | Reload config and datapacks |
| `/lootr convert` | `lootr.admin` | Convert nearby chests to Lootr |
| `/lootr custom` | `lootr.admin` | Create custom Lootr chest |
| `/lootr debug` | `lootr.admin` | Debug datapack loot tables |
| `/customchest` | `lootr.custom` | Shortcut to create custom chest |

## Permissions

- `lootr.admin` - Full admin access
- `lootr.custom` - Create custom chests
- `lootr.break` - Break Lootr chests
- `lootr.bypass` - Bypass all restrictions

## Config

```yaml
settings:
  auto-convert: true
  particles: true
  sounds: true
  
loot:
  refresh-hours: 0  # 0 = one time per player
  
protection:
  prevent-break: true           # Prevent breaking in Survival/Adventure
  require-permission-to-break: false
  op-double-break: true         # OP must break twice to confirm
  prevent-hopper: true          # Prevent hopper extraction
  prevent-explosion: true       # Prevent explosion damage
  
messages:
  break-warning: "&c&l[!] &eThis chest is instanced per-player. You cannot break it!"
