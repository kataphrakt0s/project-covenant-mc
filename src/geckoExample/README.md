# GeckoLib integration examples

These unregistered examples compile against Minecraft 26.1.2, NeoForge 26.1.2.126
and GeckoLib 5.5.2 during `check`/`build`. They are **not packaged**, registered,
spawned, or added to normal game runs. The wolf's native entity/renderer/controller
integration remains separate; its five animation clips and rig are now shipped assets.

- `ExampleEntity`: a native mob with an instance cache, looping idle and a
  server-triggered action. A real entity also needs its registry entry,
  attributes, AI and spawn rules.
- `ExampleBlock` / `ExampleBlockEntity`: native block-entity ownership and
  server-triggered animation. Animation alone needs no block ticker. A real
  asset also needs block/block-entity/item registration and a blockstate JSON
  pointing to an empty native block model with a particle texture.
- `ExampleItem`: native item properties retain weapon/tool behavior. Stack size
  one and GeckoLib's server-assigned stack ID distinguish animated stacks. Call
  `playAcceptedAction` from an already validated server action; animation
  callbacks must not deal damage or spend resources.
- `client/ExampleRenderers`: invoke entity/block registration from the physical
  client `EntityRenderersEvent.RegisterRenderers` listener. Item rendering is
  created lazily by `createGeoRenderer`; GeckoLib's cache checks the physical side
  before invoking the client factory. No early nullable provider is installed.

The defaulted model identifiers match the disposable `gecko-smoke` probes:
`covenant:gecko_probe_entity`, `covenant:gecko_probe_block`, and
`covenant:gecko_probe_item`; the defaulted models add their kind subdirectory.
They resolve files beneath `assets/covenant/geckolib/models/<kind>/` and
`geckolib/animations/<kind>/`, plus `textures/<kind>/`. Animated item definitions
use `minecraft:special` with `geckolib:geckolib` and a native display base model.
Do not ship these examples without replacing the probe resource names.

After `scripts/Blockbench.ps1 gecko-smoke`, validate its printed asset directory:

```powershell
. .\scripts\Environment.ps1
Invoke-ProjectGradle @('geckoAssetTest', '-PgeckoAssets=.local/blockbench/<run>/assets/covenant')
```

`GeckoAssetTest` uses the actual GeckoLib JSON loader, model/animation baker and
animation processor. It requires all three probe rigs and nine clips, checks
bone names/duration/loop modes, samples linear and step boundaries, and verifies
that exported Catmull-Rom keys stay Catmull-Rom after runtime baking. Missing
arguments/files and lost animations fail the command. It deliberately does not
claim rendering, networking, or gameplay validation.

GeckoLib 5.5.2's Catmull-Rom interior control points can differ from Blockbench's
preview. The test records the pinned runtime's interior result. Prefer linear
and step keys when exact preview parity matters; inspect Catmull-Rom motion in
Minecraft before accepting a production asset. The export helper also adapts
the MCP's outer `post`/`lerp_mode` envelope to the runtime's `vector` form so that
the interpolation is not silently discarded.

`WolfAnimationAssetTest` is a separate production-asset check included in
`check`/`build`. Run `Invoke-ProjectGradle @('wolfAnimationAssetTest')` for a
focused pass. It loads the shipped `original_wolf` rig, five clips and atlas
through GeckoLib, samples 34,926 runtime values, verifies loop endpoints and
neutral action recovery, and checks diagonal strides, bite interpolation,
recoil direction and the held death pose. It does not register the wolf or
establish Minecraft client rendering or controller behavior.
