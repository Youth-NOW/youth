# WorldLimit User Documentation

## Plugin Introduction
WorldLimit is a powerful world access control plugin that restricts players from entering specific worlds through a combination of various conditions. The plugin features a modular design, supports multiple versions, and provides a wealth of customization options.

## Feature Highlights
- Multi - version support (1.8 - 1.20+)
- Asynchronous processing mechanism
- Multilingual support (en_US, zh_CN, ja_JP)
- Variable parsing engine (supports PlaceholderAPI)
- Rich condition types
- Performance monitoring system
- Automatic exception handling

## Dependencies
- **Required**
    - Spigot/Paper 1.8+
- **Optional**
    - PlaceholderAPI (recommended, for variable support)

## Installation Steps
1. Download the plugin jar file.
2. Place it in the `plugins` folder of the server.
3. Restart the server.
4. The configuration file will be generated automatically.

## Permission Nodes
```yaml
worldlimit.admin:
  description: Allows the use of all WorldLimit commands
  default: op

worldlimit.debug:
  description: Allows the use of debug commands
  default: op

worldlimit.bypass:
  description: Allows bypassing world access restrictions
  default: op
```

## Command System
### Basic Commands
- `/wl reload` - Reload the configuration.
- `/wl add <world_name> <type> [parameters]` - Add a world access condition.
- `/wl remove <world_name> <sequence_number>` - Remove a specified condition.
- `/wl list [world_name]` - View the list of world conditions.
- `/wl info <world_name>` - View detailed information about a world.
- `/wl lang <list|set|default>` - Language settings.
- `/wl debug <option>` - Debug tools.

### Command Examples for Condition Types
```
/wl add world_nether item DIAMOND_SWORD 1 "§6Nether Sword"
/wl add world_nether permission worldlimit.world.nether
/wl add world_nether variable %player_level% >= 10
/wl add world_nether cooldown 300
```

## Configuration Files
### config.yml
```yaml
# Plugin language settings (supported: en_US, zh_CN, ja_JP)
language: zh_CN

# Debug mode
debug: false

# Version adapter settings
version:
  show_info: true
  force_enable: false

# Cache settings
cache:
  variable_duration: 1000
  variable_enabled: true
```

### world_rules.yml
```yaml
worlds:
  world_nether:
    enabled: true
    message:
      deny: "&cYou cannot enter this world!"
      cooldown: "&eYou need to wait {time} seconds before you can enter again!"
    conditions:
      - type: item
        item: "DIAMOND_SWORD:0"
        amount: 1
        name: "§6Nether Sword"
        lore:
          - "§7This sword allows you to"
          - "§7enter the Nether"
        message: "&cYou need a Nether Sword to enter!"
      
      - type: permission
        permission: "worldlimit.world.nether"
        message: "&cYou do not have permission to enter this world!"
      
      - type: variable
        variable: "%player_level%"
        operator: ">="
        value: 10
        message: "&cYou need to reach level 10 to enter!"
      
      - type: cooldown
        time: 300
        permission_bypass: "worldlimit.bypass.cooldown"
        message: "&eYou need to wait {time} seconds before you can enter again!"
```

## Explanation of Condition Types
### 1. Item Check (item)
Checks if the player holds a specific item.
```yaml
type: item
item: "item_ID:data_value"
amount: quantity
name: "item_name" (optional)
lore: [description_line_1, description_line_2] (optional)
message: "failure_prompt"
```

### 2. Permission Check (permission)
Checks if the player has a specific permission.
```yaml
type: permission
permission: "permission_node"
message: "failure_prompt"
```

### 3. Variable Check (variable)
Checks if the variable value meets the condition.
```yaml
type: variable
variable: "%variable_name%"
operator: "operator"
value: "target_value"
message: "failure_prompt"
```

Supported operators:
- `==` equals
- `!=` not equals
- `>` greater than
- `<` less than
- `>=` greater than or equal to
- `<=` less than or equal to
- `range` range check

### 4. Cooldown Check (cooldown)
Sets the cooldown time for entering a world.
```yaml
type: cooldown
time: cooldown_time (seconds)
permission_bypass: "bypass_permission" (optional)
message: "failure_prompt"
```

## Built - in Variables
- `%player_health%` - Player's current health
- `%player_max_health%` - Player's maximum health
- `%player_food%` - Player's hunger level
- `%player_level%` - Player's experience level
- `%player_exp%` - Player's experience points
- `%player_world%` - Player's current world
- `%player_gamemode%` - Player's game mode
- `%player_flying%` - Player's flying state

## Precautions
1. **Version Compatibility**
    - The plugin will automatically detect the server version and use the appropriate adapter.
    - If there are version compatibility issues, it will automatically degrade to the basic mode.
2. **Performance Optimization**
    - Enabling variable caching can improve performance.
    - The asynchronous processing mechanism ensures that it does not affect the main thread.
3. **Error Handling**
    - The plugin includes an automatic error - handling mechanism.
    - It will automatically isolate problematic variables.
    - Detailed information can be viewed through the debug command.
4. **Configuration Files**
    - After modifying the configuration, use `/wl reload` to reload.
    - It is recommended to back up the configuration file before modification.
5. **Multilingual Support**
    - Different languages can be set for each player.
    - The language files support UTF - 8 encoding.

## Frequently Asked Questions
1. **Q**: Why can't some variables be used?
    - **A**: Ensure that PlaceholderAPI is installed and the variable provider plugin is running properly.
2. **Q**: How do I add custom variables?
    - **A**: You can add them through PlaceholderAPI or develop a plugin to implement the VariableProvider interface.
3. **Q**: Why is there no prompt when the condition check fails?
    - **A**: Check if the `message` field in the condition configuration is set correctly.
4. **Q**: How do I disable the restrictions for a certain world?
    - **A**: Set `enabled` to `false` for the corresponding world in `world_rules.yml`.

## Technical Support
If you encounter problems, you can:
1. Use the `/wl debug` command to collect debug information.
2. Check the error logs in the server console.
3. Ensure that you are using the latest version of the plugin.

## Change Log
### v1.0.0
- Implemented version isolation system.
- Added configuration management system.
- Implemented variable parsing engine.
- Completed the core of world access detection.
- Added administrator command system.
- Implemented asynchronous processing pipeline.
- Added exception monitoring layer.
- Completed multilingual support.
