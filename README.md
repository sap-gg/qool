# sapgg-item-remover

Some (mostly) QoL additions for the `sap.gg`-SMP server (hopefully) without unfair game advantages.

## Features

### /dev/null

Deletes items when **picking them up** if they match a filter. 
Helps for example when farming to delete Cobblestone, 
or when mob farming to delete Rotten Flesh etc. 
without overloading the server.

#### Syntax

* `/dev/null clear` - clears all materials
* `/dev/null recover` - recover */dev/null*'ed items
* `/dev/null run`:
  * `/dev/null run inv` - delete items in inventory matching /dev/null
  * `/dev/null run range=10` - delete items on ground of range 10 matching /dev/null
  * `/dev/null run container` - delete items from single container in eyesight matching /dev/null
  * `/dev/null run container->` - delete items from multiple containers in eyesight matching /dev/null
* `/dev/null ,` - show items in /dev/null
* `/dev/null +ITEM1,+ITEM2,-ITEM3 -ITEM4` - adds `ITEM1` and `ITEM2` to the /dev/null & removes `ITEM3` and `ITEM4` from the /dev/null

#### Signs

Signs can be used to switch between filters very quickly.
Write `[devnull]` into the 2nd line and in the 3rd line the operation (`clear` or `preset`) and then right-click the sign.

* `clear` - clears the filters
* `preset` - adds all items from a container, e.g. a chest behind the sign, to the filter

#### Examples

**Gold Farm**: Has by-products such as gold swords, golden axes, rotten flesh and crossbows.
> `/dev/null +GOLDEN_SWORD +GOLDEN_AXE +ROTTEN_FLESH +CROSSBOW`

**Ender Pearl Farm**: Enderpearl farms, which are used e.g. as XP farm, generate many enderpearls, which overload the server.
> `/dev/null run range=8` (from inside the ender pearl farm)

---

### /trash

Opens a trash can (54 slot inventory). All items added to this inventory will be voided.

> **Warning**: voided items **can not be restored** in contrast to `/dev/null recover`

---

### /dump

The `/dump` command can be used to move items from the inventory to an *adjacent* container (e.g. box) by command.


#### Syntax

* `/dump [-v] <Type> <Orientation> [Filter]` - general syntax

#### Types

* `INVENTORY` - all items in the inventory (excluding armor and hot bar)
* `HOTBAR` - all items from the hot bar

Multiple types can be combined using `,`.

* `INVENTORY` - moves all items from the inventory
* `INVENTORY,HOTBAR` - moves all items from inventory and hot bar

#### Orientation

* UP
* DOWN
* NORTH
* SOUTH
* WEST
* EAST

Multiple orientations can be combined using `,`.

* `NORTH` - moves items to north
* `NORTH,WEST` - first moves all items to north and then remaining items to west (if the inventory from north is full, for example)

#### Filters

* Any Material from ([https://jd.papermc.io/paper/1.16/org/bukkit/Material.html](https://jd.papermc.io/paper/1.16/org/bukkit/Material.html))
* `AIR` - only transfers matching items

---

### Elevator

Can be used to move quickly on the Y axis.

#### Building

Simply build two (or more) daylight sensors on the same Y axis.

#### Usage

* `JUMP` - go to the next upper daylight sensor
* `SNEAK` - go to the next lower daylight sensor

#### Illegal Block

If the following blocks are above the daylight sensor, the elevator cannot be used:

* `LAVA`
* `WATER`

---

### Hopper Filters

> Inspired by [LiveOverflow](https://youtu.be/Gi2PPBCEHuM?t=224).

#### Building

Simply place a hopper with a filter as the name

#### Filters


* `*_sword` - only move swords (item types ending with *s*)
* `gold*` - only move items with their type starting with *gold*
* `totem_of_undying` - only move Totem of Undying
* `!diamond_sword` - move all items except diamond swords
* `!*_sword` - move all items except swords

> **Note**: Filters are case-insensitive

Multiple filters can be combined with `,\s*`.

* `*_SWORD,DIAMOND*` - move swords and items with their type starting with *diamond*

---

### /magnet

Attracts items lying around like a magnet.

#### Syntax

* `/magnet <Radius> [Filter...]`
* `/magnet 25` - move all items on the ground in a 25 blocks radius to the player
* `/magnet 10 GOLD_NUGGET` - move all gold nuggets on the ground in a 10 block radius to the player
* `/magnet 10 GOLD_NUGGET,DIAMOND` - move all gold nuggets and diamonds on the ground in a 10 block radius to the player

> **Note**: The maximum radius is **1000 Blocks**.