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

## Loggers

### TNTCart

logs TNT minecart info useful for designing TNT minecart duping looters

- chat (default): only prints info to chat
- sound: only plays sounds when a tnt minecart gets destroyed (bell on success, fart on failure)
- full: prints to chat and plays sounds

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
