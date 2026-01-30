# Trust System

**Type:** Server System (Social + Ability Mechanics)

## Overview
A team-based trust system allowing players to create teams, invite members, and form alliances. Members and allies become immune to ability damage from each other, while normal PvP damage remains unblocked. No build/claim mechanics—purely social + ability interactions.

## Team System

### Commands

#### Team Commands (Members)
```
/trust team create <id>                → Create team with unique ID (anyone)
/trust team accept <team_id>           → Accept team invite (anyone)
/trust team leave                      → Leave team (broadcast to team) (members)
/trust team leave-silent               → Leave team (silent, no notification) (members)
/trust team info                       → View team members (members)
```

#### Team Commands (Leader Only)
```
/trust team modify displayname <name>  → Set display name (leader)
/trust team invite <player>            → Send invite (pending until accepted) (leader)
/trust team kick <player>              → Remove member (leader)
/trust team transfer <player>          → Transfer leadership (leader)
/trust team delete                     → Disband team (leader)
```

#### Allies Commands (Leader Only)
```
/trust allies add <team_owner>         → Request ally (leader)
/trust allies accept <team_owner>      → Accept ally request (leader)
/trust allies remove <team_owner>      → Break ally (leader)
/trust allies list                     → View allied teams (leader)
```

### Team Rules
- **Team ID:** Globally unique UUID
- **Display Name:** Separate from ID, customizable, can have duplicates
- **Team Creation:** 
  - Once a player JOINS a team, they CANNOT CREATE new teams
  - Can still ACCEPT invites to join additional teams (if config allows multiple membership)
- **Max Team Size:** Configurable in `systems.yml`
- **Creator Behavior:** If creator leaves/deletes, entire team disbands
- **Membership:** A player can be in multiple teams simultaneously (configurable)
- **Invites:** Pending until accepted; forgotten on server restart
- **Command TabComplete Filtering:** Only autocomplete applicable commands based on player state
  - `/trust team leave` not shown if not in a team
  - `/trust team create` not shown if already in any team
  - `/trust team accept` not shown if not invited
  - Leader-only commands not shown for regular members
  - Ally commands not shown for non-leaders

## Allies System

### Commands (Leader Only)
```
/trust allies add <team_owner>         → Request ally (pending)
/trust allies accept <team_owner>      → Accept ally request (only if pending invite)
/trust allies remove <team_owner>      → Break ally (only if in alliance)
/trust allies list                     → View allied teams (leader only)
```

### Ally Rules
- **Mutual Acceptance:** Both team leaders must accept alliance
- **Pending Status:** Requests forgotten on server restart
- **Multiple Allies:** Team can have multiple allied teams
- **Ability Immunity:** Members of allied teams cannot damage each other with abilities
- **Visibility:** Allies NOT shown in locator bar
- **Leader Visibility:** Both allied team leaders can see each other in ally relationships
- **Member Visibility:** Regular team members do NOT see ally information

## Ability Immunity

### Damage Blocking
- Abilities deal **0 damage** to:
  - Team members
  - Members of allied teams
- **Normal PvP damage applies** (swords, bows, melee still work)

### Locator Bar
- **Non-members:** No players shown
- **Team members:** Shows only teammates (not allies, not other players)
- **Allies:** Not displayed in locator bar

## Data Storage

All data saved in `/data/` directory (plugin data folder) as YAML files.

### File Structure
```
/data/
  teams.yml               → Team data (id, display name, creator_uuid)
  team_members.yml        → Team membership (team_id → player_uuids)
  allies.yml              → Ally relationships (team_id pairs + status)
  [invites session-only]  → Pending invites (RAM only, cleared on restart)
```

### teams.yml
```yaml
team-uuid-1:
  id: team-uuid-1
  displayName: Example Team
  creatorUuid: player-uuid
  createdAt: 2024-01-26T12:00:00Z

team-uuid-2:
  id: team-uuid-2
  displayName: Another Team
  creatorUuid: player-uuid-2
  createdAt: 2024-01-26T13:00:00Z
```

### team_members.yml
```yaml
team-uuid-1:
  - player-uuid-1
  - player-uuid-2
  - player-uuid-3

team-uuid-2:
  - player-uuid-4
  - player-uuid-5
```

### allies.yml
```yaml
team-uuid-1:
  team-uuid-2: accepted
  team-uuid-3: pending

team-uuid-2:
  team-uuid-1: accepted
```

## Configuration

### `systems.yml`
```yaml
trust_system:
  team:
    max_per_player: 2          # Configurable max teams
    max_members: 25            # Configurable team size
  allies:
    enabled: true
```

## Implementation Notes
- Session-only storage for invites/ally requests (ephemeral, cleared on restart)
- Ability damage check: Query team_members + allies before applying ability damage
- Locator bar filter: Only display teammates (not allies)
- No build/claim/permission system—purely ability interaction layer
