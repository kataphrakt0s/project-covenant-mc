# Project Covenant

Minecraft **26.1.2** · NeoForge **26.1.2.126** · Java **25**

Project Covenant is a Minecraft-native RPG mod that adds class-based combat,
character progression, and profession mastery to the sandbox. The goal is to
make fighting, gathering, building a character, and exploring Minecraft's
dimensions part of a connected progression experience.

Vanilla activities and recipes remain available without Covenant levels.
Minecraft's XP and enchanting levels stay separate from Covenant progression.
The longer-term design builds on existing vanilla and modded equipment through
gear customization, specialized crafting, and more structured world challenges.

## Current state

Covenant is an **early development prototype**. The combat progression and
profession foundations are playable, while much of the design is still planned.
Balance values are provisional. Use fresh worlds and player saves for testing;
migration from earlier prototype save formats is unsupported.

### Classes and combat

| Class | Combat style | Resource |
| --- | --- | --- |
| **Stalker** | Bow attacks, precision, and traps | Focus |
| **Reaver** | Melee attacks, bleeds, and movement combos | Rage |
| **Weaver** | Elemental spellcasting and control | Mana |

Each class has its own skill tree, with active attacks and auras available now.
Earn skill points through character levels, learn up to eight distinct skills,
and assign them to eight combat bindings. Prerequisites and level requirements
shape each build; crafted respec items let you reallocate points. Planned skills
remain visible in the trees but cannot be purchased or used.

### Progression and professions

- **Character progression:** Combat XP rewards eligible damage and support
  contributors. Character progression begins when you choose a class and has a
  default level cap of 60, with level caps of 20 for XP earned in the Overworld,
  40 in the Nether, and 60 in the End. Dimension caps govern XP gains; all
  dimensions remain open to exploration.
- **Four independent professions:** Mining, Logging, Farming, and Crafting can
  progress before class selection, each with a default level cap of 20. Eligible
  ore and tree harvesting, mature crops, breeding, and enchanting-table use
  award profession XP.
- **Profession abilities:** Profession points, allocation menus, and respec are
  implemented. Logging's Offensive Mastery is the first usable ability; the
  remaining profession abilities are planned.
- **Shared foundations:** Progression is saved and controlled by the server.
  Datapacks configure progression balance and activity rewards. Equipment stats
  and per-item Affix Page storage exist, with debug commands for testing.

Expanded profession abilities, the Covenant Workbench, gear rarity, affix
acquisition and customization, salvaging, and Covenant-specific dungeons,
bosses, and world events remain future work.

## Trying the prototype

In a development world, hold **G** to open the radial menu. Hover an entry and
release G, or left-click it, to open **Skills**, **Stats**, or **Professions**.
Choose your class in Stats, then learn skills and assign them through the
Skills window's **Keybindings** tab. Apply allocation changes while out of
combat; closing a window discards unapplied changes. These menus do not pause
the world.

Default skill bindings are **Q, R, F, C** and **Shift+Q/R/F/C**. Controls are
rebindable under **Controls > Key Binds > Project Covenant**. By default, Covenant
clears vanilla Drop Item on Q and Swap Hands on F to free those keys; this can
be disabled in `config/covenant-client.toml`.

Covenant is developed against a clean NeoForge instance. ATM11 and Apotheosis
compatibility are future targets; neither is a dependency or a validated
integration.

## Building and development

Follow [development setup](docs/DEVELOPMENT.md) to configure Java 25 and the
local environment before using the project scripts:

```powershell
.\scripts\Dev.ps1 build
.\scripts\Dev.ps1 test
```

`build` compiles, runs geometry checks, and packages
`build/libs/covenant-0.1.0-dev.jar`. `test` runs the dedicated-server GameTests.
The development guide also covers deployment to an isolated Prism instance.

## Documentation

- [Project vision and game design](<Project-Covenant/Project Covenant - GDD.md>)
  and the [Obsidian vault index](Project-Covenant/README.md) describe the intended
  experience. Internal vault links use Obsidian wikilinks.
- [Progression vertical slice](<Project-Covenant/Progression Vertical Slice.md>)
  defines the current development scope.
- [Project context](CONTEXT.md), [progression implementation](docs/PROGRESSION.md),
  [skills](docs/RPG_SKILLS.md), and [professions](docs/PROFESSIONS.md) describe
  implemented behavior and its limits.
- [Validation results](docs/VALIDATION.md) record automated evidence and pending
  manual checks. [AGENTS.md](AGENTS.md) contains repository working conventions.

## Licensing

**Source code:** Original Project Covenant source code is licensed under the
[MIT License](LICENSE). Developers may use, modify, fork, redistribute, and
commercially use it subject to that license.

**Logo:** The Project Covenant logo (`src/main/resources/mod-icon.png`) is
excluded from MIT and is not licensed for reuse by this repository. See
[ASSET-LICENSE.md](ASSET-LICENSE.md) for details and permission requests.

**Attribution:** Redistributors must preserve the applicable MIT copyright
and license notices. We also request voluntary attribution to Project Covenant
and a link to the [original repository](https://github.com/kataphrakt0s/project-covenant-mc)
when incorporating or adapting its code. This request adds no license condition.
See [NOTICE](NOTICE).

**Minecraft and third-party content:** These remain the property of their
respective owners, under their respective licenses and terms, and are not
covered by the Project Covenant MIT license. This includes Minecraft source
code, APIs, textures, models, sounds, icons and other assets; Mojang/Microsoft
trademarks and branding; and third-party libraries, dependencies and mod assets.
References to Minecraft identifiers, APIs, registries, resources or mechanics
do not grant Project Covenant ownership of those materials. References to
Minecraft and other mods describe compatibility or intended use and do not
imply ownership or endorsement.

The upstream NeoForged template notice is preserved in
[TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt). Gradle wrapper files retain their
own copyright and Apache-2.0 notices, including the license embedded in the
wrapper JAR. Existing third-party licenses and notices continue to apply.
