# HazeStore

A Minecraft plugin featuring a rotating store powered by Haze, with GUI, weighted item pool, purchase logging, and full MiniMessage support. Supports both MMOItems and vanilla items, with CoinsEngine and PlaceholderAPI integration.

## Dependencies
- MMOItems (optional, for custom items)
- CoinsEngine
- PlaceholderAPI

## Commands

| Command | Description |
|---------|-------------|
| `/hs` or `/hs open` | Opens the shop |
| `/hs open [player]` | Opens the shop for a specific player |
| `/hs admin reload` | Reloads the configuration |
| `/hs admin forcerotate` | Forces the item pool rotation |

## Permissions

| Permission | Description |
|------------|-------------|
| `hs.open` | Allows opening the shop |
| `hs.admin` | Allows using admin commands |

## Pool Configuration

### MMOItems
```yaml
item1:
  type: "mmoitems"
  mmoitems_type: "SWORD"
  mmoitems_id: "EPIC_SWORD"
  price: 500
  weight: 50
```

### Vanilla Items
```yaml
item5:
  type: "vanilla"
  material: "DIAMOND"
  amount: 16
  price: 100
  weight: 80
```

### Fields
- `type`: Item type (`mmoitems` or `vanilla`)
- `mmoitems_type`: MMOItem type (only for `mmoitems`)
- `mmoitems_id`: MMOItem ID (only for `mmoitems`)
- `material`: Vanilla material (only for `vanilla`)
- `amount`: Item amount (only for `vanilla`, default: 1)
- `price`: Item price
- `weight`: Weight for random selection (higher weight = higher chance to appear)
