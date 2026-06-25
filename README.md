# Material Preparer

[![EN](https://img.shields.io/badge/English-blue.svg)](README.md)
[![中文](https://img.shields.io/badge/简体中文-blue.svg)](README.zh-CN.md)

A Minecraft Fabric mod that helps you automatically collect and organize building materials.

## Features

### 📦 Smart Item Collection
- Configure a list of required items and automatically collect them from nearby chests
- Supports precise movement, taking only the needed quantity
- Can ignore items already in your inventory, collecting only what's missing

### 🗺️ Container Exploration
- Automatically explores nearby containers and caches item information
- Supports blacklist marking to skip unwanted chests
- Supports output container marking – automatically stores items when inventory is full

### 📋 Multiple Import Methods
- Import item lists from Litematica schematic files
- In-game file selection interface with subfolder browsing support
- CSV format storage for easy manual editing

### ⚡ Configurable Operation Speed
- Adjustable container click interval
- Prevents issues caused by clicking too quickly

### 🎨 Debug Visualization
- Displays debug bounding boxes around containers
- Different colors distinguish different container types
- Makes it easy to see which containers have been explored

## Installation

1. Install [Fabric Loader](https://fabricmc.net/) and [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
2. Install [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu) and [YACL](https://www.curseforge.com/minecraft/mc-mods/yacl)
3. Place the mod's `.jar` file into the `mods` folder

## Usage Guide

### Configuring the Item List

1. Open Mod Menu (default keybind: `Mod Menu` shortcut)
2. Find Material Preparer and click "Configure"
3. Add the items and quantities you need in the "Item List" category
4. Click Save

### Collecting Items

1. Set up your item list
2. Ensure there are chests nearby containing the items you need
3. Press "Collect Items"
4. The mod will automatically open nearby chests and collect the items

### Importing from Litematica

1. Place your `.litematic` schematic file into the `schematics` folder
2. Open the configuration interface
3. In the "General" category, find "Load item list from Litematica"
4. Select the schematic file you want to load from the file selection interface
5. Click the "Load" button

### Marking Containers

**Blacklist**:
1. Aim your crosshair at the chest you want to mark
2. Press "Toggle Blacklist"
3. Press again to unmark it

**Output Container**:
1. Aim your crosshair at the chest you want to use as output
2. Press "Toggle Output Container"
3. When your inventory is full, items will be automatically stored in the output container

## Configuration

### General Settings

| Option | Description | Default |
|--------|-------------|---------|
| Minimum Click Interval | Minimum interval between container clicks (ticks), 0 = no limit | 0 |
| Always Quick Move | Whether to use Shift+click for quick movement | Enabled |
| Ignore Existing Items | Whether to collect only items missing from inventory | Disabled |

### Container Management

| Action | Description |
|--------|-------------|
| Toggle Blacklist | Add/remove the targeted block from the blacklist |
| Toggle Output Container | Set/unset the targeted block as output container |
| Clear All Marks | Clear all container marks |

### Debug

| Option | Description | Default |
|--------|-------------|---------|
| Show Debug Borders | Whether to display debug borders around containers | Disabled |

## Keybindings

All keybindings can be customized in the configuration interface.

| Function | Default Key |
|----------|-------------|
| Open Configuration Interface | Unbound |
| Explore All Nearby Containers | Unbound |
| Collect Items | Unbound |
| Load Item List from Litematica | Unbound |
| Load Item List from CSV | Unbound |
| Open Recent Crafting Table | Unbound |
| Toggle Debug Borders | Unbound |
| Toggle Always Quick Move | Unbound |
| Toggle Ignore Existing Items | Unbound |
| Toggle Blacklist | Unbound |
| Toggle Output Container | Unbound |
| Clear All Marks | Unbound |

## File Locations

- Configuration file: `config/material_preparer/config.json5`
- Item list: `config/material_preparer/item_list.csv`
- Schematics directory: `schematics/`

## Project Documentation

Want to learn more about implementation details, architecture, and future plans? Check out the [project documentation](docs/PROJECT.md).

## Supported Versions

- Minecraft 1.21.1
- Fabric Loader

## Dependencies

- Fabric API
- Mod Menu
- YACL (Yet Another Config Lib) v3

## License

MIT License
