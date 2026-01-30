# Awakened Lichblade

**Source Pack:** Blades of Majestica 3D Weapon Pack

## Base Item
- Material: `DIAMOND_SWORD`
- Model: `lumen:awakened_lichblade`
- Display Name: `<dark_purple><bold>Awakened Lichblade`
- Base Damage: Default
- Base Attack Speed: Default
- Unbreakable: Yes
- Static model (no progression variants)

## Abilities

### Right-Click: Sonic Boom
- **Range:** 20 blocks
- **Damage:** 10 HP
- **Effect:** Spawns sonic boom particles every 0.25 blocks from player to target
- **Cooldown:** 25 seconds
- **Special:** Ignores armor, pierces through blocks

### Off-Hand (F-Key): Blinding Pulse
- **Radius:** 50 blocks
- **Effect:** Applies Blindness with distance-based duration
  - 0-2 blocks: 60 seconds
  - 50 blocks: 1 second
  - Linear scaling between
- **Cooldown:** 80 seconds
- **Separate cooldown from Sonic Boom**

## Ritual Recipe

Grid layout (0,1,2 = top row; 3,4,5 = middle row; 6,7,8 = bottom row):

```
Row 0: Bone Block (8)    | Sculk Shrieker (8) | Bone Block (8)
Row 1: Sculk Block (16)  | Warden Heart (1)   | Sculk Block (16)
Row 2: Bone Block (8)    | Diamond Sword (1)  | Bone Block (8)
```

## Crafting Component

### Warden Heart
- Custom crafting ingredient
- Texture: Anatomical heart shape, dark navy/black with glowing teal/cyan veins and accents
- Source: 25% chance drop on Warden death (player kill only)
- Used in Lichblade ritual recipe (qty: 2)

## Theme
- Deep Dark / Warden-inspired
- Dark purple and teal color scheme
- Sonic/vibrational damage
- Sensory disruption (blindness mechanic)
