# Project Covenant

Minecraft **26.1.2** · NeoForge **26.1.2.125** · Java **25**

A minimal mod foundation. ATM11 is a future target; current testing uses an
isolated blank NeoForge instance in Prism Launcher.

Hold **G** in a world to open the ten-slot radial menu. Move the cursor over a
number to highlight its placeholder; release G or press Escape to close.
Rebind it under **Controls > Key Binds > Project Covenant**. Actions are not wired
up yet, and the menu leaves the world running.

```powershell
.\scripts\Dev.ps1 build
.\scripts\Dev.ps1 prism
```

Prism instance: **Project Covenant - Dev (26.1.2)**.
Build output: `build/libs/covenant-0.1.0-dev.jar`.

Read [development setup and testing](docs/DEVELOPMENT.md),
[project context](CONTEXT.md), and [validation results](docs/VALIDATION.md).
Codex sessions start with [AGENTS.md](AGENTS.md).
