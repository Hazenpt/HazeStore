# HazeStore

A rotating "mystery shop" plugin for Paper. A weighted item pool swaps on a
configurable timer, players buy from a fully configurable GUI, and admins manage
everything live from an in-game editor without ever touching a file.

Everything is driven by MiniMessage-formatted config files and supports vanilla
items, MMOItems and ItemsAdder in the same pool.

    made with love by haze


## Highlights

    Rotating pool        Weighted random selection on a configurable timer,
                         with broadcast warnings before each rotation.

    Multiple pools       One YAML file per pool, each with its own timer,
                         items and optional GUI override.

    Configurable GUI     MiniMessage formatting, size 27 / 36 / 45 / 54,
                         per-slot layout and decorative filler blocks.

    Hidden & VIP items   Items masked until clicked to reveal; VIP items gated
                         behind a permission. Reveal resets every rotation.

    Buy confirmation     Per-slot configurable confirm menu that shows the
                         item with its exact store lore.

    In-game editor       Create, edit and delete pools and items live.
                         The store refreshes instantly, no reload needed.

    Per-rotation limits  max-purchases resets each rotation, with a fully
                         configurable "sold out" icon.

    Player heads         Any config icon can be a custom head (base64 texture
                         or a real player's skin).

    Integrations         CoinsEngine economy, PlaceholderAPI expansion,
                         MMOItems and ItemsAdder — all optional.


## Compatibility

    Minecraft          1.19 - 1.21.x+  (single JAR)
    Server software    Paper (native Adventure / MiniMessage API)
    Java               17 or newer

All soft dependencies are optional. A missing dependency only disables the
feature that needs it; the plugin keeps running.

    MMOItems                        enables  type: mmoitems  items
    ItemsAdder                      enables  type: itemsadder  items
    CoinsEngine / ExcellentEconomy  economy: balance read and currency take
    PlaceholderAPI                  balance reading + %hazestore_...% placeholders

Economy note: purchases charge the player by running the console command
`<currency> take <player> <price>`, and the balance is read through
PlaceholderAPI (`%coinsengine_balance_raw_<currency>%`). This is designed
around CoinsEngine. Set `currency` in config.yml to your CoinsEngine currency id.


## How it works

Each pool lives in its own YAML file under `pools/`. A repeating task fires
every second and, when the timer expires, picks new items from the pool using
weighted random selection (higher weight = higher chance). The chosen items
fill the `pool`-type slots of the GUI and are cached for fast opening.

On every rotation:

    - the selected item IDs are written to data.yml
    - per-item purchase counts reset
    - hidden-item reveals reset
    - open store GUIs refresh automatically

Admins can trigger a rotation at any time with `/hs admin forcerotate`.


## Commands

    /hs                            hs.open    Open the default store
    /hs open [pool]                hs.open    Open a specific pool
    /hs open <pool> <player>       hs.admin   Open a pool for another player
    /hs admin edit [pool]          hs.admin   Open the live in-game editor
    /hs admin reload               hs.admin   Reload all configs from disk
    /hs admin forcerotate [pool]   hs.admin   Force an immediate rotation


## Permissions

    hs.open     everyone   Open the store
    hs.vip      op         See and buy VIP-gated items
    hs.admin    op         Editor, reload, force rotate, and bypass
                           hidden / VIP masking (preview mode)


## Configuration

All files are generated in `plugins/HazeStore/` on first run.
Run `/hs admin reload` to apply changes without restarting.

### config.yml

Global settings: rotation timer, currency, broadcasts, sounds, and the shared
hidden / VIP / sold-out icons.

```yaml
prefix: "<gray>[<gold>Hazestore</gold>]</gray>"
rotation: "1d 0h 0m"          # supports d / h / m   e.g.  "12h 30m"
currency: "gold"              # CoinsEngine currency id
default-pool: "default"

broadcast:
  enabled: true               # master switch for all broadcasts
  rotation: true              # announce when a pool rotates
  warnings: true              # announce the timed warnings below

# Icon shown for hidden items a player has not revealed yet.
# For a head: material "PLAYER_HEAD" + texture (base64) OR owner (player name).
hidden-item:
  material: "BARRIER"
  texture: ""
  owner: ""
  name: "<gray>Secret item</gray>"
  lore:
    - "<dark_gray>???"

# Icon shown for VIP items to players without hs.vip.
vip-item:
  material: "BARRIER"
  texture: ""
  owner: ""
  name: "<gold>VIP item</gold>"
  lore:
    - "<gray>Unlock this with <gold>VIP</gold>"

# Icon shown when an item is sold out for the current rotation.
sold-out-item:
  material: "BARRIER"
  texture: ""
  owner: ""
  name: "<red>Sold out</red>"
  lore:
    - "<gray>This item is sold out"

messages:
  no-permission:    "<red>You do not have permission.</red>"
  not-enough-coins: "<red>Not enough {currency}!</red>"
  purchased:        "<green>Purchased {item} for {price} {currency}!</green>"
  already-sold:     "<red>This item is sold out!</red>"
  store-rotated:    "<gold>The shop has changed! Check /hs</gold>"
  warnings:
    60: "<yellow>Shop changes in 1 hour!</yellow>"
    10: "<yellow>Shop changes in 10 minutes!</yellow>"
    1:  "<yellow>Shop changes in 1 minute!</yellow>"

sounds:
  gui-open:      { sound: "BLOCK_CHEST_OPEN",            volume: 1.0, pitch: 1.0 }
  purchase:      { sound: "ENTITY_PLAYER_LEVELUP",       volume: 1.0, pitch: 1.0 }
  hidden-reveal: { sound: "ENTITY_EXPERIENCE_ORB_PICKUP", volume: 1.0, pitch: 1.2 }
  force-rotate:  { sound: "BLOCK_NOTE_BLOCK_PLING",      volume: 1.0, pitch: 1.0 }
```

### gui.yml

Controls the store window. `pool`-type slots are where rotated items appear;
the number of `pool` slots equals how many items each rotation shows. Any other
slot is a static decorative filler.

```yaml
title: "<b><color:#2B2B2B>haze store</color></b>"
size: 54                      # 27 / 36 / 45 / 54
lore:
  price: "<gold>Price: </gold><yellow>{price} {currency}</yellow>"
  purchase-hint: "<gray>Click to <green>purchase</green>!</gray>"
slots:
  21: { type: "pool",   material: "BLACK_STAINED_GLASS_PANE", name: " " }
  22: { type: "pool",   material: "BLACK_STAINED_GLASS_PANE", name: " " }
  23: { type: "pool",   material: "BLACK_STAINED_GLASS_PANE", name: " " }
  0:  { type: "filler", material: "BLACK_STAINED_GLASS_PANE", name: " " }
```

### confirmation.yml

The buy-confirmation menu. Every entry supports player heads
(`material: "PLAYER_HEAD"` + `texture` or `owner`).

```yaml
enabled: true
title: "<gold>Confirm Purchase</gold>"
size: 27

confirm: { slot: 11, material: "LIME_WOOL", name: "<green>Confirm</green>" }
cancel:  { slot: 15, material: "RED_WOOL",  name: "<red>Cancel</red>"  }
info:    { slot: 13 }                          # real item, exact store lore

filler:  { material: "GRAY_STAINED_GLASS_PANE", name: " " }
slots: {}                                      # optional per-slot overrides
```

### pools/<name>.yml

Each file in `pools/` is an independent pool; the file name is the pool name.
Open it with `/hs open <name>`.

```yaml
items:

  # Vanilla item
  diamond_pack:
    type: "vanilla"
    material: "DIAMOND"
    display-name: "<aqua>Diamond Pack</aqua>"   # optional MiniMessage name
    amount: 16
    price: 500
    weight: 50                  # higher = more likely to appear
    hidden: false
    vip: false
    max-purchases: -1           # per-rotation limit; -1 = infinite
    slot: -1                    # -1 = auto-place into a "pool" slot

  # VIP-only item
  vip_apple:
    type: "vanilla"
    material: "GOLDEN_APPLE"
    price: 250
    weight: 20
    vip: true                   # requires hs.vip

  # MMOItems cosmetic, hidden, one buy per rotation
  cosmetic:
    type: "mmoitems"
    mmoitems_type: "COSMETICI"
    mmoitems_id: "ALIJOLLY"
    price: 350
    weight: 10
    hidden: true
    max-purchases: 1

  # ItemsAdder item
  custom:
    type: "itemsadder"
    itemsadder-id: "namespace:my_item"
    price: 300
    weight: 30
```

A pool file may also include a `gui:` section (`title`, `size`, `slots`) to
override the global gui.yml for that pool only.

### Item fields

    type            vanilla / mmoitems / itemsadder  (inferred if omitted)
    material        Bukkit material name                       (vanilla)
    mmoitems_type   MMOItems type                              (mmoitems)
    mmoitems_id     MMOItems item id                           (mmoitems)
    itemsadder-id   ItemsAdder namespaced id                   (itemsadder)
    display-name    Custom MiniMessage name shown in the store  (optional)
    amount          Stack size, default 1                       (vanilla)
    price           Cost in the configured currency
    weight          Selection weight; higher = more likely to appear
    vip             Requires hs.vip; others see the vip-item icon
    hidden          Masked until clicked to reveal; resets each rotation
    max-purchases   Buy limit per rotation; -1 = infinite
    slot            Fixed GUI slot, or -1 to auto-place into pool slots


## Player heads

Any configurable icon (hidden-item, vip-item, sold-out-item, and every
confirmation entry) can be a player head:

```yaml
material: "PLAYER_HEAD"
texture: "<base64>"     # the "Value" field from e.g. minecraft-heads.com
# or
owner: "PlayerName"     # uses a real player's current skin
```

`texture` and `owner` are ignored if `material` is not `PLAYER_HEAD`.


## PlaceholderAPI

Expansion id: `hazestore`. Requires PlaceholderAPI installed.

    %hazestore_default_pool%                        Current default pool name
    %hazestore_<pool>_rotation_time%                Time until rotation (Xd Xh Xm)
    %hazestore_<pool>_rotation_time_seconds%        Seconds until rotation
    %hazestore_<pool>_rotation_time_formatted%      Human-friendly countdown
    %hazestore_<pool>_item_<i>_name%                Item name/material at index i
    %hazestore_<pool>_item_<i>_price%               Item price
    %hazestore_<pool>_item_<i>_purchases%           Purchases this rotation
    %hazestore_<pool>_item_<i>_max%                 Max purchases
    %hazestore_<pool>_item_<i>_soldout%             yes / no
    %hazestore_<pool>_item_<i>_type%                Item type



