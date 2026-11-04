---
name: covenant-development
description: Build, validate, and deploy the Project Covenant NeoForge mod for Minecraft 26.1.2 to its isolated Prism development instance. Use for changes and testing in this repository.
---

# Covenant development

Read `AGENTS.md` and `CONTEXT.md` in the repository root. Use
`docs/DEVELOPMENT.md` for commands and lifecycle checks relevant to the change.

The default system Java may be incompatible. Use `scripts/Dev.ps1`, which selects
JDK 25 through ignored `.local/environment.json`. To invoke other Gradle tasks,
dot-source `scripts/Environment.ps1` first and use the checked-in wrapper.

Build with `scripts/Dev.ps1 build`. Deploy with `scripts/Dev.ps1 deploy`, or build,
deploy, and launch with `scripts/Dev.ps1 prism`. These operations target the
marker-owned `Covenant-26_1_2-Dev` instance, not the user's vanilla or ATM11 instances.
Close the development game before replacing its JAR. If pins or ownership checks
fail, inspect the local config and instance metadata before changing anything.

Check APIs against the pinned sources and official NeoForge 26.1 documentation.
Use focused tests where behavior warrants them. For common gameplay, verify
dedicated-server loading and applicable persistence/network lifecycles; for client
changes, verify the actual rendered behavior. No gameplay or GameTests exist in
the starting scaffold, so do not present empty Gradle test tasks as test coverage.

Record important decisions in `CONTEXT.md` and durable workflow changes in
`docs/DEVELOPMENT.md`. Report the build result, in-game observations, and validation
limits distinctly. Do not publish, push, upgrade the target, or add ATM11-specific
dependencies unless the user asks.
