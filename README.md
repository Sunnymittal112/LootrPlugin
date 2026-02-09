# 🎁 LootrPlugin

**Per-player unique loot from structure chests — Minecraft Lootr Mod for Spigot/Paper!**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20+-green.svg)](https://www.spigotmc.org/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)

---

## 📖 Overview

LootrPlugin brings the popular **Lootr mod functionality** to Spigot/Paper servers. Each player gets their own unique loot from naturally generated structure chests (temples, dungeons, ancient cities, etc.) — no more rushing to loot chests first!

### ✨ Key Features

- 🎲 **Per-Player Loot** — Each player sees different items in the same chest
- 🏛️ **Structure Detection** — Automatically detects temple/dungeon/ancient city chests
- 💾 **Persistent Storage** — Loot saved across server restarts
- 🔒 **Full Protection** — Prevents breaking, explosions, hopper extraction, piston movement
- 🎨 **Visual Effects** — Particle indicators on unopened chests
- ⚙️ **Highly Configurable** — 20+ config options
- 🛡️ **Admin Tools** — Complete command suite for management

---

## 🚀 Installation

1. **Download** the latest `LootrPlugin-1.0.0.jar` from [Releases](https://github.com/Sunnymittal112/LootrPlugin/releases)
2. **Place** the JAR in your server's `plugins/` folder
3. **Restart** the server
4. **Configure** `plugins/LootrPlugin/config.yml` (optional)

### Requirements
- **Minecraft Version:** 1.20+
- **Server Software:** Spigot, Paper, Purpur
- **Java Version:** 17 or higher

---

## 🎮 How It Works

### Player Experience

1. **Player A** finds a desert temple chest
2. Opens it → Gets random loot (e.g., 3 diamonds, 5 iron ingots)
3. **Player B** opens the *same* chest → Gets *different* loot (e.g., 1 emerald, 8 gold ingots)
4. **Player A** reopens → Sees the *same* items as before (3 diamonds, 5 iron)

### Technical Flow
