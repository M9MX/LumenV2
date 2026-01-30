# Soulrender

**Source Pack:** Custom Design (Soul/Death Theme)

## Base Item
- Material: `DIAMOND_SWORD`
- Model: `lumen:soulrender`
- Display Name: `<dark_aqua><bold>Soulrender`
- Base Damage: Default
- Base Attack Speed: Default
- Unbreakable: Yes
- Static model (no progression variants)

## Abilities

### Right-Click: Summon Skeletal Horse
- **Duration:** 90 seconds
- **Cooldown:** 180 seconds
- **Speed:** 16.0 blocks/sec
- **Health:** 25.0 hearts
- **Cooldown Calculation:** Uses millisecond-based timestamp system
  - Current time fetched as `System.currentTimeMillis() / 1000` (seconds)
  - Cooldown expiry = current time + cooldown duration (seconds)
  - Ability blocked until current time >= cooldown expiry time
  - Remaining cooldown = max(0, expiry time - current time)
- **Implementation:** Reuses DeadCompanionAbility horse summon logic directly

## Passive: Soul Harvest
- **Trigger:** Kill a player while holding Soulrender
- **Effect:** Drops player skull with victim's name and UUID
- **Skull Display:** `[Victim Name]`
- **UUID Storage:** Stored for skin application
- **Drop Chance:** 100%

## Theme
- Soul/death-inspired
- Dark cyan, dark blue, gray color scheme
- Ethereal, spectral aesthetic
- Reflects soul collection and skeletal companionship
