# Lumen V2 Systems

## Item Protection System

Protects custom items from being placed in restricted containers and tracks item locations/events.

### Configuration (systems.yml)

```yaml
item_protection:
  enabled: true      # Enable/disable entire system
  logging: true      # Enable/disable item tracking to file
```

### Protection Modes

Each item can have one of three protection modes (set in item class):

#### NONE
- No protection
- Item can be placed anywhere

#### SIMPLE
- Prevents placing item in:
  - Enderchest
  - Shulker boxes
  - Bundles
- Does NOT prevent dropping

#### STRICT
- Same restrictions as SIMPLE
- Prevents dropping the item
- **Tracks item location** when:
  - Moved in inventory
  - Dropped (if `logging: true`)
  - Despawned
  - Burned in fire/lava
  - Destroyed in explosion

### Tracked Data (item_tracking.yml)

```yaml
items:
  example_item:
    player: "PlayerName"
    location: "World: world, X: 100, Y: 64, Z: 200"
    status: "in_inventory" | "dropped" | "despawned" | "burned" | "exploded"
    cause: "LAVA" (only for burned status)
    timestamp: "2026-01-23 12:30:45"
```

### Usage in Items

```java
public class MyItem extends CustomItem {
    @Override
    protected void initialize() {
        // ... set item properties ...
        
        // Set protection mode
        setProtectionMode(ItemProtectionMode.STRICT);
    }
}
```

### Configuration Reload

```
/lumen reload systems     # Reloads systems.yml and applies new settings
/lumen reload all         # Reloads all configs including systems.yml
```

### Features

- ✅ Enabled/disabled toggle
- ✅ Logging can be toggled independently (tracking writes to file)
- ✅ Console warnings logged for all events
- ✅ Hot reload via command
- ✅ Three protection modes
- ✅ Event tracking (drop, despawn, burn, explode)
- ✅ Multi-location support for items

## Enhancement System

Allows custom items to be enhanced with Catalyst Shards. Each shard provides a modifier to item abilities.

### Configuration (systems.yml)

```yaml
enhancement:
  enabled: true      # Enable/disable entire system
```

### Usage in Items

```java
public class MyItem extends CustomItem {
    @Override
    protected void initialize() {
        // ... set item properties ...
        
        // Make item enhancable
        setEnhancable(true);
    }
}
```

### How It Works

1. **Open Enhancement GUI**: Shift+right-click any enhancable item to open the GUI
2. **GUI Layout**:
   - 3 rows (27 slots) filled with gray glass panes
   - Slot 13 (center): Input slot for Catalyst Shards
   - Slot 4 (above center): Display showing current shard count (max 5)
3. **Add Shards**: Place Catalyst Shards in the center slot
4. **Close GUI**: When you close the GUI, the shard count is saved to the item's Persistent Data Container (replacing NBT data)
5. **Access Shard Count**: Use `EnhancementManager.getShardCount(itemStack)` to get the count

### Catalyst Shard Item

- **Material**: AMETHYST_SHARD
- **Custom Model**: `lumen:catalyst_shard`
- **Item ID**: `catalyst_shard`
- **Purpose**: Used to upgrade items with enhancements

### API Usage

```java
// Get shard count
int shards = EnhancementManager.getShardCount(itemStack);

// Set shard count
EnhancementManager.setShardCount(itemStack, 3);

// Add shards
EnhancementManager.addShards(itemStack, 1);

// Remove shards
EnhancementManager.removeShards(itemStack, 1);

// Check if item can hold more shards
boolean canAdd = EnhancementManager.canAddShard(itemStack);

// Or use ItemDataHelper for convenience
int shards = ItemDataHelper.getShardCount(itemStack);
ItemDataHelper.setShardCount(itemStack, 3);
```

### Features

- ✅ Shift+right-click to open enhancement GUI
- ✅ Visual shard count display (max 5)
- ✅ Automatic Persistent Data Container save/load on GUI close/open (replacing NBT)
- ✅ Easy API to access shard data
- ✅ Non-interactable gray glass pane decoration

## Forge System

Custom crafting system for recipes defined in custom items.

### Configuration (systems.yml)

```yaml
forge:
  enabled: true      # Enable/disable entire system
  recipes:           # Enable/disable specific recipes by item ID
    example_item: true
    another_item: true
```

### How It Works

1. **Right-click Smithing Table**: Opens a menu with two buttons
   - **FORGE**: Opens the custom crafting GUI
   - **Smithing Table**: Opens the vanilla smithing GUI

2. **Forge GUI Layout** (6-row inventory, 54 slots):
   - **Crafting Grid** (slots 10,11,12,19,20,21,28,29,30): 3x3 grid for recipe items
   - **Output Border** (slots 14,15,16,23,25,32,33,34): Green/red glass panes showing recipe validity
   - **Quick Craft Slots** (slots 17,26,35): Shows up to 3 craftable items based on grid contents
   - **Output Slot** (slot 24): Take completed item here
   - **Recipe GUI** (slot 51): Placeholder for recipe browser
   - **Empty Slots**: Gray glass panes (non-interactable)

3. **Recipe Validation**: 
   - Checks if items in grid match a recipe exactly (correct items, correct amounts, correct positions)
   - Green border = valid recipe, Red border = invalid recipe
   - Only items with recipes are checked

4. **Quick Craft**:
   - Displays items you can craft from current grid contents
   - Click a quick craft slot to populate the grid and take items from inventory
   - Automatically places correct amounts in correct positions

5. **On Close**: Items in crafting grid are returned to player inventory

### Recipe Setup in Custom Items

```java
public class MyItem extends CustomItem {
    @Override
    protected void initialize() {
        // ... set item properties ...
        
        // Create recipe
        Recipe recipe = new Recipe("my_recipe", RecipeType.NORMAL);
        
        // Set grid items (row, col, itemId, amount)
        recipe.setSlot(0, 0, "item_1", 1);
        recipe.setSlot(0, 1, "item_2", 2);
        recipe.setSlot(1, 0, "item_3", 1);
        
        // Set result
        recipe.setResult("my_item");
        
        // Attach to item
        setRecipe(recipe);
    }
}
```

### Recipe Types

- **NORMAL**: Regular crafting - item goes directly to player inventory
- **RITUAL**: Ritual crafting - triggers special ritual effects (future feature)

### API Usage

```java
// Check if recipe is enabled
boolean enabled = ForgeSystem.getInstance().isRecipeEnabled("item_id");

// Enable/disable a recipe
ForgeSystem.getInstance().setRecipeEnabled("item_id", false);
```

### Features

- ✅ Right-click smithing table to access forge
- ✅ 6-row custom GUI with organized layout
- ✅ Recipe validation with visual feedback (green/red borders)
- ✅ Quick craft system showing craftable items
- ✅ Exact recipe matching (position and amount matters)
- ✅ Items returned on GUI close
- ✅ Per-recipe enable/disable configuration
- ✅ Support for NORMAL and RITUAL recipe types
- ✅ Hot reload via config

## Ritual System

Handles special ritual-type recipe crafting with visual effects and server-wide cooldowns.

### Configuration (systems.yml)

```yaml
ritual:
  enabled: true              # Enable/disable entire system
  duration_seconds: 600      # Ritual duration in seconds (default: 10 minutes)
  cooldown_minutes: 30       # Server-wide cooldown between rituals (default: 30 minutes)
```

### Disabled Ritual System Behavior

If `ritual.enabled: false`:
- RITUAL recipe type items craft like NORMAL mode (immediate, no effects)
- Item goes directly to player inventory
- **Still registers in ItemCraftsManager** - only 1 of each ritual item can be crafted per server
- Recipe ingredients are consumed normally
- No visual effects, no cooldown, no barrier blocks

### How It Works

1. **Recipe Type**: Recipe must be set to `RecipeType.RITUAL` in the custom item's recipe
2. **Starting Ritual**: Player clicks output slot with valid RITUAL recipe
   - Item uniqueness check: Verifies item hasn't been crafted by anyone on server yet
   - If check passes: Ritual starts, player's inventory closes
3. **Ritual Phases** (0-50% progress):
   - Expanding rings and rotating rectangles with particle effects
   - Visual indicators showing ritual progress
4. **Item Convergence** (50-100% progress):
   - Recipe item `ItemDisplay` entities spawn on outer circle (8 positions)
   - Items animate toward center
   - Speed calculated so all items converge at exactly ritual end time
   - Purple particle trails left by moving items
5. **Ritual Completion**:
   - Lightning strike and explosion effects
   - Barrier blocks spawn at ritual location with reward item display
   - Player clicks barrier to collect completed item
   - Server-wide cooldown begins (30 minutes)

### Server-Wide Cooldown

- Once ANY ritual completes, the ENTIRE SERVER must wait before next ritual
- Default: 30 minutes
- All players see cooldown message with remaining time if they attempt ritual during cooldown
- Cooldown message: "The server must wait X minutes before the next ritual can begin."

### Item Uniqueness

- Each RITUAL recipe item can only be crafted once across the entire server
- Second attempt will be blocked with message: "This item has already been crafted by someone else."
- Uses `ItemCraftsManager.hasBeenCrafted()` to check

### Reward Collection

1. Ritual completes and creates barrier blocks at ritual location
2. `ItemDisplay` entity floats above barrier with reward item (rotates continuously)
3. Player right-clicks the barrier to collect reward
4. Reward item is given to player and registered in `ItemCraftsManager`
5. Barrier blocks and display disappear

### Recipe Setup in Custom Items

```java
public class MyRitualItem extends CustomItem {
    @Override
    protected void initialize() {
        // ... set item properties ...
        
        // Create ritual recipe
        Recipe recipe = new Recipe("my_ritual_recipe", RecipeType.RITUAL);
        
        // Set grid items (these will be displayed converging during ritual)
        recipe.setSlot(0, 0, "ingredient_1", 1);
        recipe.setSlot(0, 1, "ingredient_2", 2);
        recipe.setSlot(1, 0, "ingredient_3", 1);
        
        // Set result
        recipe.setResult("my_item");
        
        // Attach to item
        setRecipe(recipe);
    }
}
```

### API Usage

```java
// Start a ritual manually
RitualSystem ritualSystem = forgeSystem.getRitualSystem();
ItemStack[] recipeItems = { /* items from recipe grid */ };
ritualSystem.startRitual(player, location, resultItem, itemType, recipeItems);

// Check if location is a reward block
boolean isReward = ritualSystem.isRewardBlock(location);

// Get reward item at location
ItemStack reward = ritualSystem.getRewardItem(location);

// Get reward item type at location
String itemType = ritualSystem.getRewardItemType(location);

// Remove reward block
ritualSystem.removeRewardBlock(location);
```

### Configuration Reload

```
/lumen reload systems     # Reloads systems.yml including ritual duration
/lumen reload all         # Reloads all configs
```

### Features

- ✅ Visual particle effects during ritual
- ✅ Item convergence animation (50-100% progress)
- ✅ Server-wide cooldown tracking
- ✅ Item uniqueness enforcement (one craft per server)
- ✅ Reward block interaction with barrier blocks
- ✅ Rotating item display with automatic cleanup
- ✅ Configurable ritual duration
- ✅ BossBar countdown timer
- ✅ Hot reload via config
- ✅ Recipe items spawn on outer ring and converge to center with calculated speeds

## 4. Item Protection System

## 5. Enhancement System

## 6. Forge System

## 7. Additional Features
