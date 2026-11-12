# Project Covenant

Read `CONTEXT.md` before feature work. Use `skills/covenant-development/SKILL.md`
for the build, validation, and Prism deployment workflow. See `docs/DEVELOPMENT.md`
for machine setup and commands. Read `docs/TESTING.md` before adding or changing
tests; use its shared fixtures and focused validation commands.

For 3D model work, when available locally, use `skills/covenant-modeling/SKILL.md`
and `docs/BLOCKBENCH.md`. These modeling tools, docs and sources are Git-ignored
and are not included in a fresh checkout. Use the `covenant-bbmodel` headless stdio MCP to edit,
validate, convert, export and render `.bbmodel` files without starting Blockbench.
Load the installed upstream Blockbench skills as relevant, adapting their desktop
tool examples to the discovered headless schemas. Keep editable sources in
ignored `models/`; previews and tool dependencies stay in ignored `.local`/`.tools`.
For animated entities, block entities and items, follow `docs/GECKOLIB.md` and
the pinned GeckoLib 5 workflow; validate exports before wiring runtime consumers.

Use `skills/grill-plan/SKILL.md` when the user asks to "grill me", stress-test a
plan, or work down a design tree. Ask one question at a time with a recommendation,
check the existing code first, and record decisions in the relevant design docs.

## Engineering conventions

- Target Minecraft Java **26.1.2**, NeoForge, and Java **25**. Pinned versions live
  in `gradle.properties` and `build.gradle`; do not upgrade them implicitly.
- ATM11 is an eventual target. Do not add pack dependencies or compatibility
  shims until requested. Develop against the isolated blank NeoForge instance.
- Use the smallest implementation that satisfies the requested behavior. Prefer
  Minecraft/NeoForge registries, events, data components, attachments, resources,
  and existing entity/block-entity systems over a new framework or custom ECS.
- Keep gameplay authoritative on the logical server. Separate client rendering,
  input, and screens from common code so dedicated servers can load the mod.
- Prefer event-driven work. Avoid global per-tick scans, chunk forcing, and
  recurring allocations in hot paths. Measure consequential performance claims.
- Use data-driven recipes, loot, tags, and worldgen when applicable. Keep registry
  IDs and saved data stable; document migration implications before changing them.
- Validate network input on the server. Preserve state correctly across save/load,
  reconnects, death, and dimension changes when the feature uses those lifecycles.
- Document player/designer-facing configuration with JavaDoc and NeoForge config
  comments, including units, defaults, and constraints.
- Use official documentation and the resolved 26.1.2 sources to check APIs;
  examples for older Minecraft releases may no longer compile.
- Run focused checks and `scripts/Dev.ps1 build`; report what was actually tested.
  Compilation alone does not establish in-game behavior or server compatibility.
- Keep `.tools`, `.local`, Gradle caches, accounts, tokens, worlds, and logs out of
  source control. Preserve unrelated changes and existing Prism instances.
- Update context when decisions or workflows change. Do not fill context files
  with a transcript of routine work. No publishing or remote pushes without a request.
