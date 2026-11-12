# Testing and fixture guide

Use the smallest check that exercises the changed behavior while iterating. Run
the full gate once the change is ready. All commands use the pinned Java 25 and
NeoForge/Minecraft versions through `scripts/Environment.ps1`.

## Daily loop

```powershell
.\scripts\Dev.ps1 fast                         # No game server: standalone checks + catalogue audit
.\scripts\Dev.ps1 list-tests                   # Discover IDs grouped by owning Java suite
.\scripts\Dev.ps1 list-tests -Tests 'pet_*'     # Preview a selection
.\scripts\Dev.ps1 test -Tests 'pet_*'           # One dedicated server, only matching tests
.\scripts\Dev.ps1 test -Tests 'roster_config'   # One exact test
.\scripts\Dev.ps1 build                        # Fast checks + production JAR and packaging audit
.\scripts\Dev.ps1 verify                       # Build + ALL required dedicated tests
```

`-Tests` accepts one ID or wildcard (`*` and `?`); an omitted namespace becomes
`covenant_test:`. Quote patterns in PowerShell. An unknown/empty selection fails
before the server starts. `verify` rejects filters so a focused run cannot be
mistaken for the full gate. `test` without a filter runs every required test.
Equivalent Gradle tasks are `fastTest`, `listGameTests`, `gameTest`, `build` and
`verify`; use `-PgameTests=covenant_test:pet_*` for direct filtered invocations.

Standalone tasks record successful outputs under `build/reports/standalone/`.
Gradle skips them when their runtime classes and resource inputs are unchanged.
Use Gradle `--rerun-tasks` when deliberately rechecking unchanged inputs. Do not
use `clean`, disable the configuration cache, or delete the world/cache as a
routine first step. The normal Gradle `test` task has no framework tests; its
`NO-SOURCE` result is not coverage.

Dedicated tests always execute. They write `build/reports/gameTest/results.xml`
and `runs/gametest/logs/latest.log`. The previous XML is removed before launch.
Success requires a clean process exit, exactly the selected number of XML test
cases and no failures/errors/skips. A focused run replaces the previous report;
record the command and scope alongside results. Wait for Gradle's final result,
including server shutdown. Never run two GameTest processes in this checkout at
once: they share a disposable world and report path.

Focused runs still pay native server startup and shutdown costs. In this pinned
version shutdown explicitly enables world saving; do not bypass it with test
mixins or terminate the server after seeing the pass line. Use the standalone
loop for changes that do not require the world, then wait for a clean dedicated
exit when testing server behavior.

## Source sets and ownership

| Location | Purpose | Default gate |
| --- | --- | --- |
| `src/geometryTest/java/dev/covenant/client` | Pure Java geometry, layouts and presentation state; package mirrors production for package-private access | `fast`, `build` |
| `src/assetTest/java/dev/covenant/test/assets` | Shipped assets parsed/baked with the pinned runtime, without a game server | `fast`, `build` |
| `src/gameTest/java/dev/covenant/test` | Native registries, entities, packets, persistence, combat and datapack reloads | `test`, `verify` |
| `src/gameTest/java/dev/covenant/test/fixture` | Shared test-only setup and lifecycle plumbing | Compiled with GameTests |
| `src/gameTest/resources/data/covenant_test` | Test instances, structures and isolated environments | Audited by `fast`; loaded by GameTests |
| `src/geckoExample/java` | Compile-checked integration examples, not test suites | Compiled by `fast`, `build` |

The historical `geometryTest` name includes pure presentation-state tests such
as Apply feedback. Do not bootstrap Minecraft for those checks. Prefer a small
executable `main` using assertions that throw (not Java's disabled-by-default
`assert`). Add standalone checks through `standaloneCheck` in
`gradle/testing.gradle`, include every external file read as an input, and add
the task to the appropriate aggregate. The disposable `geckoAssetTest` remains
opt-in with `-PgeckoAssets=<export-directory>`; it is not shipped-asset coverage.

Keep feature assertions in an owning `<Feature>GameTests` class. Reusable
mechanics belong in `fixture`, not in another feature's private nested fixture.
Local fixtures may compose the shared helpers and set feature-specific state.
Do not move test helpers into `src/main` to make access easier. All tests,
examples and test datapacks remain outside normal client/server runs and the
production JAR; `checkProductionJar` enforces the packaging boundary.

## Registering and configuring a GameTest

1. Add a static `void` method accepting `GameTestHelper` in its owning suite.
   Name it for an observable contract, not the implementation's private method.
2. Add one explicit method-reference registration in `CovenantTestMod`:
   `FUNCTIONS.register("feature_behavior", () -> FeatureGameTests::behavior);`
   Keep this form; the lightweight catalogue audit and test listing read it.
3. Add `test_instance/feature_behavior.json` with the same function ID:

```json
{
  "type": "minecraft:function",
  "function": "covenant_test:feature_behavior",
  "environment": "minecraft:default",
  "structure": "covenant_test:empty",
  "max_ticks": 100,
  "setup_ticks": 0,
  "required": true
}
```

`max_ticks` is a positive budget in game ticks, not wall-clock milliseconds.
Choose it from the behavior's actual deadlines plus bounded readiness margin;
100 is an example, not a mandatory timeout. Keep required tests single-attempt;
do not hide flaky setup with retries or optional/manual-only flags. Omitted
`setup_ticks` and `required` retain vanilla defaults (0 and true) in older files.
Do not add a setup delay to mask missing readiness.

Use `minecraft:default` for isolated local tests. Tests that reload datapacks or
change global server configuration require a dedicated `covenant_test:*_reload`
environment, restoration in `finally`, and no dependent test ordering. Existing
`pet` and `combat_slice` environments group their native simulation tests. An
environment name controls batching; an empty `all_of` wrapper does not itself
clean up players, tickets, listeners, files or state. Tests in the same batch can
overlap. A new structure requires an explicit update to the catalogue audit.

`validateGameTests` rejects missing/duplicate registrations, orphan JSON,
function/filename disagreement, missing methods/environments, invalid timeout
or retry policy, and copied connection bootstraps. Java compilation and server
loading still check types and the full native data codecs. Run `fast` and the
specific new test before the full gate.

## Fixture contracts

`TestPlayers.create(helper)` returns an ordinary connected Survival
`ServerPlayer`. The connection declares itself in-memory and negotiates NeoForge
mock channels before joining, including GeckoLib payloads. The overload accepting
a packet observer shares the same setup; clear captured setup packets before
asserting an action's output. Do not duplicate cookies, embedded channels or
`placeNewPlayer`. The configuration-handshake rejection test is deliberately an
exception: it tests an unnegotiated connection before a player exists.

Class, position, gravity, attributes, resource balances, equipment and learned
skills remain explicit in the local fixture. `FixtureSkills.learn` is trusted
setup; tests of allocation must use the real purchase boundary. The vanilla
`makeMockServerPlayerInLevel()` remains suitable for storage-only cases, but its
Creative behavior cannot prove Survival spending or spectator rejection. Use
`ServerPlayer.doTick()` for the player lifecycle; use the native connection tick
when testing movement reconciliation. Do not run both for the same simulated
tick. Teleport acknowledgements remain explicit when that protocol matters.

Use `TestChunks` for a bounded fixture area beyond the structure:

```java
var chunks = new TestChunks(helper);
var origin = helper.absolutePos(new BlockPos(1, 100, 1));
chunks.load(origin.getX() - 4, origin.getZ() - 4,
        origin.getX() + 20, origin.getZ() + 4);
helper.startSequence()
        .thenWaitUntil(chunks::assertReady)
        .thenExecute(() -> { /* arrange actors and perform the action */ })
        .thenExecuteAfter(20, () -> { /* assert the observable result */ })
        .thenSucceed();
```

The helper uses a unique ticket per fixture, deduplicates chunks, waits for the
pinned loading future with the server executor, and releases every ticket at
completion. Readiness still requires entity ticking. For entity queries, also
assert the target is present in the actual query before measuring a hit or AI
deadline. Retrying readiness must be side-effect-free: do not spawn actors or
cast repeatedly inside `thenWaitUntil`. Production gameplay never uses these
tickets or forces chunks for tests.

Every asynchronous local fixture registers `TestCleanup.onFinish(helper,
this::close)` and has an idempotent `close()`. Keep `try/finally` or
try-with-resources for early release. The completion listener is the fallback
for success, failure and timeout. `runBeforeTestEnd` only schedules a callback
at timeout minus one; it is **not** teardown. The pinned `GameTestHelper.testInfo`
reflection bridge is confined to `TestCleanup` because the public helper lacks
a listener accessor. Recheck this bridge when explicitly upgrading Minecraft.

Each fixture owns its players, entities/projectiles, teams, modified blocks,
event listeners and temporary files/packs. Restore original block states for
terrain edits, unregister listeners, cancel gameplay before removing players,
and release tickets. Register cleanup before performing fallible setup; make
partial-construction cleanup safe. `TestPlayers` independently removes any still
registered player and releases its embedded channel at test completion. Avoid
global entity removal, shared static mutable state, fixed player UUIDs, and
cross-test dependence. Native persistence tests may intentionally preserve a
documented artifact outside the disposable world; do not delete that artifact
as general teardown.

## Assertions and evidence

Test a player-observable behavior or authority/lifecycle invariant. Include the
relevant rejection path and verify rejected actions leave costs/state intact.
Prefer native public actions and events; keep narrowly necessary reflection in
one fixture seam and explain why the production boundary cannot exercise it.
Use deterministic inputs and concrete expected values; include expected/actual
values in failures. Preserve the original failing assertion when cleaning up.
Avoid assertions that merely repeat a private implementation calculation.

For data changes, test valid values, invalid boundaries and reload preservation.
For time-dependent behavior, start the deadline after fixture readiness and use
the production interval. Never increase a timeout or loosen an assertion until
the failure is classified as setup, timing, implementation or expectation drift.
Repeat a focused test only to investigate a specific timing concern; retries do
not make an intermittent failure acceptable.

Report the exact command, selected scope, final result and material limitations.
Full dedicated automation does not prove rendered appearance, client startup,
GUI/input feel, real multiplayer latency, retained-world restart, compatibility
or performance. The user owns manual client playtesting unless they request
client automation. See `docs/VALIDATION.md` for historical evidence and pending
manual checks; keep routine run transcripts out of `CONTEXT.md`.

API references: [NeoForge GameTests](https://docs.neoforged.net/docs/misc/gametest/)
and the resolved 26.1.2 source JAR. Current online examples may differ from the
pinned checkout; verify native signatures locally before changing fixtures.
