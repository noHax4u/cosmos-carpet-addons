# Cosmos Carpet Addons

Our additions to [carpet mod](https://github.com/gnembon/fabric-carpet)

Ask questions and request features here: https://discord.gg/KmDsW6tKbs

## Rules

### fullrangecfinditem

Allows all players to use ClientCommands' fast `/cfinditem` search across containers
in loaded chunks within render distance, without opening them or requiring operator permissions.
Also searches container entities and the player's own ender chest inventory.

Install Cosmos Carpet Addons and Carpet on the server, and ClientCommands on the
client. Clients do not need this addon or Carpet. Enable with
`/carpet fullrangecfinditem true`; disable with `/carpet fullrangecfinditem false`.
Changes take effect for connected players immediately and apply to new logins and respawns.
The `--click-inventories` flag still selects the normal nearby-container search.

This advertises level-2 permissions to clients so unmodified ClientCommands selects
its fast search. Actual server permissions are unchanged; other client features
that check this permission indicator may also become available. Non-operators can
only query container inventories in chunks sent to them and their own ender chest inventory.

- Type: `boolean`
- Default value: `false`
- Accepted values: `true`, `false`
- Categories: `CCA`, `survival`

### ghostProjectileFix

Partially prevents projectiles from desyncing after being pushed by a piston
by sending their position and motion for the specified number of ticks after being pushed.

Overrides ghostEntityFix for projectiles

- Type: `int`
- Default value: 0
- Accepted values: [0 - 10]
- Categories: `CCA`, `bugfix`

### ghostEntityFix

Partially prevents all entities from desyncing after being pushed by a piston
by sending their position and motion for the specified number of ticks after being pushed.

- Type: `int`
- Default value: 0
- Accepted values: [0 - 10]
- Categories: `CCA`, `bugfix`

### lightLevelManipulator

Lets you edit block light and sky light levels directly, without light propagation.

While enabled:

- Block changes no longer trigger light updates (similar to carpet's old `lightUpdates ignored`).
  Lighting of newly generated chunks still works.
- Using a light item (`minecraft:light`) does not place a light block. It only sets the light level
  of that position to the item's level. Items with the `minecraft:enchantment_glint_override`
  component set to `true` set the sky light instead of the block light.
- The `/lightmanipulator` command is available (requires op permission level 2):

```
/lightmanipulator give (block|sky) [<level>] [<targets>]
/lightmanipulator give portal (x|z) [<blockLight>] [<skyLight>] [<targets>]
/lightmanipulator fill <from> <to> (block|sky) <level>
/lightmanipulator generate <from> <to> (block|sky) [normalized|raw|offset|center] <expression>
```

`give` hands out a light item for the given level (default 0); the `sky` variant has the glint override set.

`give portal` hands out a nether portal item with the given axis. Placing it places a nether portal block
without a frame (neighbours are not updated, so it doesn't break right away) and, while the rule is enabled,
sets the block light (default 0) and sky light of that position. A level of `-1` keeps the current level
(default for sky light). Portal items are purple stained glass panes with custom data: clients with this
mod installed render them as actual nether portals, vanilla clients see a purple stained glass pane.

`generate` works like WorldEdit's `//generate`: the expression is evaluated for every position
and its result is used as the light level (rounded and clamped to 0 - 15). A negative result
leaves the position unchanged. Available variables are `x`, `y`, `z` and `cur` (the current light level).
The coordinate mode selects what `x`, `y` and `z` are:

- `normalized` (default): -1 to 1 across the area, 0 at its center
- `raw`: world coordinates
- `offset`: relative to the position the command is run from
- `center`: relative to the center of the area, in blocks

Operators: `+ - * / % ^` (or `**`), `== != < <= > >=`, `&& || !`, `cond ? a : b`.
Functions: `abs sqrt cbrt sin cos tan asin acos atan atan2 sinh cosh tanh exp ln log log10
floor ceil round rint min max pow hypot clamp random`. Constants: `pi`, `e`.

Example: a glowing sphere that fades towards its edge

```
/lightmanipulator generate ~-10 ~-10 ~-10 ~10 ~10 ~10 block 15 * (1 - sqrt(x^2 + y^2 + z^2))
```

Notes:

- Areas are limited to 16,777,216 positions and must be loaded.
- Positions in sections that store no light data (e.g. high above the terrain) are skipped.
- Edited light stays until something triggers a light update there after the rule is disabled.
- Changed light is sent to all players tracking the chunk. Clients still compute light locally for
  blocks they change themselves, so they may show vanilla lighting around those blocks until the
  chunk is reloaded.

#### Axiom integration (optional)

If [Axiom](https://modrinth.com/mod/axiom) is installed on the server and on the builder's client
(together with this mod), its builder tools copy the light levels of their source blocks while the rule is
enabled, instead of recalculating light. For example, stacking a nether portal with block light 3 and sky
light 0 produces more portals with block light 3 and sky light 0. Supported tools: stack, extrude, clone
and move (including rotation and flipping). Other Axiom edits leave the existing light untouched.

Axiom is not required: without it, the integration is simply not loaded. Fabric has no way to make sure
Axiom loads before this mod, so the integration doesn't depend on load order: its mixins only target Axiom
and are only applied when Axiom is installed.

Clients with this mod also stop relighting their own block changes while the rule is enabled, so what
they see matches the server.

#### Litematica integration (optional, client side)

With [Litematica](https://modrinth.com/mod/litematica) installed on the client, the "Save schematic" screen gets
an "Include light levels" option. It stores the block and sky light level of every position in the schematic
(a `cca:Light` entry in each sub-region). Unmodified Litematica clients load these schematics normally and
just ignore the light data (it is dropped if they save the schematic again).

The schematic verifier then also verifies light levels: the "Light: N mismatches" button shows the count,
lists the closest mismatches when hovered, and toggles highlighting them in the world (off by default; in the
"different block" color, and not on blocks that already show another verifier marker). Fixed mismatches and
mismatches the placement no longer covers (e.g. after moving it) disappear within a second; new ones are found by
running the verifier again.

- Type: `boolean`
- Default value: `false`
- Accepted values: `true`, `false`
- Categories: `CCA`, `creative`, `command`

## Loggers

### TNTCart

logs TNT minecart info useful for designing TNT minecart duping looters

- chat (default): only prints info to chat
- sound: only plays sounds when a tnt minecart gets destroyed (bell on success, fart on failure)
- full: prints to chat and plays sounds

## Building

```
./gradlew build
```

The jar ends up in `build/libs`. Axiom and Litematica are only needed at compile time (they are downloaded
from Modrinth's Maven automatically) and are not bundled.

To test with Axiom and Litematica in the dev environment, install them into `run/mods`, then run the client
or server as usual:

```
./gradlew installDevMods
./gradlew runClient
./gradlew removeDevMods
```

`./gradlew runClientWithoutCca` starts a client with the same dependencies and `run/mods`, but without this mod,
e.g. to check that schematics saved with light data still load in unmodified Litematica.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
