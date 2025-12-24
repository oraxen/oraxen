# Migration Guide: Pack.models System

This guide explains the new `Pack.models` feature that eliminates the need for fake helper items when you need multiple models for a single item.

## What Changed?

### Before (Old Pattern)
To have a furniture with different visual states (e.g., a turntable with open/closed states), you had to create **two separate items**:

```yaml
# Main item
turntable:
  displayname: "<gray>Turntable"
  material: PAPER
  Mechanics:
    furniture:
      jukebox:
        active_stage: "turntable_active"  # References another item
  Pack:
    model: default/turntable_closed

# Fake helper item just to register the model
turntable_active:
  material: PAPER
  excludeFromInventory: true  # Hide from inventory
  Pack:
    model: default/turntable_opened
```

**Problems with this approach:**
- Clutters the item registry with fake items
- `excludeFromInventory: true` is a hack, not a clear intent
- Easy to make typos in references
- Two items to maintain instead of one

### After (New Pattern)
Now you can define all models in a single item using `Pack.models`:

```yaml
turntable:
  displayname: "<gray>Turntable"
  material: PAPER
  Mechanics:
    furniture:
      jukebox:
        active_model: opened  # References a key from Pack.models
  Pack:
    model: default/turntable_closed
    models:
      opened: default/turntable_opened
```

**Benefits:**
- Single item definition
- Clear semantic intent
- All models defined in one place
- Auto-registered as `oraxen:<itemId>/<key>` (e.g., `oraxen:turntable/opened`)

---

## How to Migrate

### Step 1: Identify Items Using the Old Pattern

Look for items with these characteristics:
- `excludeFromInventory: true`
- Only contain a `Pack:` section (no mechanics, no displayname)
- Referenced by other items via `active_stage`, `next_stage`, etc.

### Step 2: Move Models to Pack.models

For each fake helper item:

1. Copy its model path
2. Add it to the main item's `Pack.models` section with a descriptive key
3. Update any references to use the new key
4. Delete the fake helper item

### Step 3: Update Mechanic References

Change mechanic configs to use the new model keys:

| Old Config | New Config |
|------------|------------|
| `active_stage: "item_active"` | `active_model: active` |

---

## Examples

### Jukebox Furniture

**Before:**
```yaml
turntable:
  Mechanics:
    furniture:
      jukebox:
        active_stage: "turntable_opened"  # Another item ID
  Pack:
    model: default/turntable_closed

turntable_opened:
  excludeFromInventory: true
  Pack:
    model: default/turntable_opened
```

**After:**
```yaml
turntable:
  Mechanics:
    furniture:
      jukebox:
        active_model: opened  # Key from Pack.models
  Pack:
    model: default/turntable_closed
    models:
      opened: default/turntable_opened

# No more turntable_opened item needed!
```

### Multiple Models

You can define as many models as needed:

```yaml
magic_lamp:
  material: PAPER
  Pack:
    model: default/lamp_off
    models:
      on: default/lamp_on
      flickering: default/lamp_flicker
      broken: default/lamp_broken
```

These are registered as:
- `oraxen:magic_lamp` → `default/lamp_off`
- `oraxen:magic_lamp/on` → `default/lamp_on`
- `oraxen:magic_lamp/flickering` → `default/lamp_flicker`
- `oraxen:magic_lamp/broken` → `default/lamp_broken`

---

## Backward Compatibility

The old `active_stage` pattern still works! Oraxen will:

1. **First** check for `active_model` and resolve via `Pack.models`
2. **Fall back** to `active_stage` (legacy item reference) if `active_model` is not set

This means:
- Existing configs continue to work without changes
- You can migrate at your own pace
- New configs should use `Pack.models` for cleaner organization

---

## What About Plant Growth Stages?

The `Pack.models` system is designed for **visual-only model swaps** (like jukebox open/closed).

For **plant growth stages** that have different gameplay properties (different drops, different evolution times, etc.), you should still use separate items. Each growth stage may need:
- Different hitboxes
- Different drop tables
- Different evolution configurations
- Different light levels

If your growth stages are purely visual (same drops, same mechanics), you could potentially use `Pack.models`, but this requires a custom mechanic implementation.

---

## Technical Details

### How It Works

1. When parsing items, Oraxen reads `Pack.models` into `OraxenMeta.additionalModels`
2. During resource pack generation, additional model definitions are created:
   - Path: `assets/oraxen/items/<itemId>/<key>.json`
   - Content: Points to the specified model path
3. Mechanics can resolve model keys to `NamespacedKey`:
   - `active` → `oraxen:<itemId>/active`

### API Access

```java
// Get the OraxenMeta for an item
OraxenMeta meta = OraxenItems.getItemById("turntable").getOraxenMeta();

// Check for additional models
if (meta.hasAdditionalModels()) {
    Map<String, String> models = meta.getAdditionalModels();
    String openedPath = meta.getAdditionalModel("opened");
}

// Resolve to NamespacedKey for setItemModel()
String itemId = "turntable";
String modelKey = "opened";
NamespacedKey modelNsKey = new NamespacedKey("oraxen", itemId + "/" + modelKey);
itemMeta.setItemModel(modelNsKey);
```

---

## Questions?

If you have questions about migrating your configs, please:
1. Check the [Oraxen Discord](https://discord.gg/oraxen)
2. Open an issue on [GitHub](https://github.com/oraxen/oraxen/issues)

