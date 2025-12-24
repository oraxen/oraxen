# Migration Guide: Pack.models System & Inline Growth Stages

This guide explains two major improvements to Oraxen's item configuration:

1. **`Pack.models`** - Define multiple models in a single item (for furniture states)
2. **Inline `stages`** - Define all plant growth stages in a single item (for evolving furniture)

---

## Part 1: Pack.models System

### What Changed?

#### Before (Old Pattern)
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

#### After (New Pattern)
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

### How to Migrate Pack.models

#### Step 1: Identify Items Using the Old Pattern

Look for items with these characteristics:
- `excludeFromInventory: true`
- Only contain a `Pack:` section (no mechanics, no displayname)
- Referenced by other items via `active_stage`, `next_stage`, etc.

#### Step 2: Move Models to Pack.models

For each fake helper item:

1. Copy its model path
2. Add it to the main item's `Pack.models` section with a descriptive key
3. Update any references to use the new key
4. Delete the fake helper item

#### Step 3: Update Mechanic References

Change mechanic configs to use the new model keys:

| Old Config | New Config |
|------------|------------|
| `active_stage: "item_active"` | `active_model: active` |

---

## Part 2: Inline Growth Stages

### What Changed?

#### Before (Old Pattern)
To create a plant with 4 growth stages, you needed **5 separate items**:

```yaml
# The seed item
weed_seed:
  displayname: "Weed Seed"
  material: PAPER
  Mechanics:
    furniture:
      item: weed_plant_stage0  # Display model from another item
      farmland_required: true
      evolution:
        delay: 10000
        probability: 0.5
        light_boost: true
        next_stage: weed_plant_stage1  # References another item
      drop:
        loots:
          - { oraxen_item: weed_seed, probability: 1.0 }
  Pack:
    generate_model: true
    textures:
      - default/weed/seed

# Stage 0 - just for the model
weed_plant_stage0:
  material: PAPER
  excludeFromInventory: true
  Pack:
    model: default/weed/stage0

# Stage 1 - needs full config for evolution
weed_plant_stage1:
  material: PAPER
  excludeFromInventory: true
  Mechanics:
    furniture:
      farmland_required: true
      evolution:
        delay: 10000
        probability: 0.5
        light_boost: true
        next_stage: weed_plant_stage2
      drop:
        loots:
          - { oraxen_item: weed_seed, probability: 1.0 }
  Pack:
    model: default/weed/stage1

# ... stage2, stage3 ...
```

**Problems with this approach:**
- 5+ items for one plant
- Lots of duplicated config (drop, evolution settings)
- Each stage creates/destroys entities (performance impact)
- Hard to maintain and modify
- Easy to break references

#### After (New Pattern)
Now you define everything in **ONE item**:

```yaml
weed_seed:
  displayname: "Weed Seed"
  material: PAPER
  Pack:
    generate_model: true
    textures:
      - default/weed/seed
    models:
      stage0: default/weed/stage0
      stage1: default/weed/stage1
      stage2: default/weed/stage2
      stage3: default/weed/stage3
  Mechanics:
    furniture:
      barrier: false
      farmland_required: true
      initial_stage: 0
      stages:
        - model: stage0
          evolution:
            delay: 10000
            probability: 0.5
            light_boost: true
            bone_meal:
              chance: 50
          drop:
            loots:
              - { oraxen_item: weed_seed, probability: 1.0 }

        - model: stage1
          evolution:
            delay: 10000
            probability: 0.5
            light_boost: true
            bone_meal:
              chance: 50
          drop:
            loots:
              - { oraxen_item: weed_seed, probability: 1.0 }

        - model: stage2
          evolution:
            delay: 10000
            probability: 0.5
            light_boost: true
            bone_meal:
              chance: 50
          drop:
            loots:
              - { oraxen_item: weed_seed, probability: 1.0 }

        - model: stage3  # Final stage - no evolution
          drop:
            loots:
              - { oraxen_item: weed_seed, max_amount: 2, probability: 0.75 }
              - { oraxen_item: weed_leaf, max_amount: 5, probability: 0.55 }
```

**Benefits:**
- **1 item instead of 5+**
- All stage data in one place
- Model swapping (no entity recreation!) - better performance
- Per-stage drops and evolution settings
- Cleaner, easier to maintain

---

### How to Migrate Inline Stages

#### Step 1: Identify Plant Items

Look for groups of items that:
- Use `next_stage: "item_id"` in evolution config
- Have `excludeFromInventory: true` stage items
- Share similar `farmland_required`/`farmblock_required` settings

#### Step 2: Create the Unified Item

For each plant:

1. **Keep the seed item** as the main item
2. **Move all stage models** to `Pack.models`:
   ```yaml
   Pack:
     models:
       stage0: default/plant/stage0
       stage1: default/plant/stage1
       # ...
   ```
3. **Add the `stages` array** under `Mechanics.furniture`:
   ```yaml
   Mechanics:
     furniture:
       initial_stage: 0
       stages:
         - model: stage0
           evolution: { ... }
           drop: { ... }
         - model: stage1
           # ...
   ```

#### Step 3: Delete Old Stage Items

Remove the old `*_stage0`, `*_stage1`, etc. items entirely.

---

### Inline Stages Config Reference

```yaml
stages:
  - model: <key>         # Required: Reference to Pack.models key
    light: <int>         # Optional: Light level for this stage (-1 = inherit)
    evolution:           # Optional: Omit for final stage
      delay: <int>       # Ticks before evolution check
      probability: <0-1> # Chance to evolve when delay reached
      light_boost: true  # Enable light boost (or use config below)
      light_boost:
        minimum_light_level: <int>
        boost_tick: <int>
      rain_boost: true   # Enable rain boost
      rain_boost:
        boost_tick: <int>
      bone_meal: true    # Enable bone meal (50% default)
      bone_meal:
        chance: <0-100>
    drop:                # Optional: Stage-specific drops (inherits main drop if omitted)
      silktouch: true
      loots:
        - { oraxen_item: seed, probability: 1.0 }
```

---

## Examples

### Jukebox with Multiple States

```yaml
turntable:
  displayname: "<gray>Turntable"
  material: PAPER
  Pack:
    model: default/turntable_closed
    models:
      opened: default/turntable_opened
  Mechanics:
    furniture:
      jukebox:
        active_model: opened
        volume: 1.0
        pitch: 1.0
```

### Complete Plant (5 stages)

```yaml
grape_seeds:
  displayname: "Grape Seeds"
  material: PAPER
  Pack:
    generate_model: true
    textures:
      - default/grape_seeds
    models:
      stage0: default/grape/stage0
      stage1: default/grape/stage1
      stage2: default/grape/stage2
      stage3: default/grape/stage3
      stage4: default/grape/stage4
  Mechanics:
    furniture:
      farmblock_required: true
      initial_stage: 0
      stages:
        - model: stage0
          evolution: { delay: 10000, probability: 0.5, light_boost: true }
          drop: { loots: [{ oraxen_item: grape_seeds, probability: 1.0 }] }
        - model: stage1
          evolution: { delay: 10000, probability: 0.5, light_boost: true }
          drop: { loots: [{ oraxen_item: grape_seeds, probability: 1.0 }] }
        - model: stage2
          evolution: { delay: 10000, probability: 0.5, light_boost: true }
          drop: { loots: [{ oraxen_item: grape_seeds, probability: 1.0 }] }
        - model: stage3
          evolution: { delay: 10000, probability: 0.5, light_boost: true }
          drop: { loots: [{ oraxen_item: grape_seeds, probability: 1.0 }] }
        - model: stage4
          drop:
            loots:
              - { oraxen_item: grape_seeds, max_amount: 2, probability: 0.75 }
              - { oraxen_item: grape, max_amount: 2, probability: 0.55 }
```

---

## Backward Compatibility

Both old patterns still work! Oraxen will:

1. **First** check for new patterns (`active_model`, `stages`)
2. **Fall back** to legacy patterns (`active_stage`, `next_stage`)

This means:
- Existing configs continue to work without changes
- You can migrate at your own pace
- New configs should use the new patterns for cleaner organization

---

## Performance Impact

### Pack.models
No performance impact - just cleaner config organization.

### Inline Stages
**Improved performance!** The old system recreated entities on each growth stage. The new system only swaps the model texture, keeping the same entity.

---

## Server Version Requirements

- **Pack.models**: Works on all versions
- **Inline stages (optimal)**: Requires Minecraft 1.21.2+ for efficient model swapping via `item_model` component
- **Inline stages (fallback)**: Works on older versions but with a warning

---

## Questions?

If you have questions about migrating your configs, please:
1. Check the [Oraxen Discord](https://discord.gg/oraxen)
2. Open an issue on [GitHub](https://github.com/oraxen/oraxen/issues)
