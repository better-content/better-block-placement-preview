# Better Block Placement Preview

## Scope

Adds an advisory, server-authoritative RBP support prediction to RotaVision block placement ghosts. Teal marks a supported proposal, amber marks a predicted fall or crush, and white marks an unavailable prediction. Vanilla-invalid placement remains red; the preview never places blocks or runs RBP physics.

Requires Forge 1.20.1, RotaVision 1.0.2, Realistic Block Physics 1.0.0, and Realistic Physics 1.0.1.

Build with `./gradlew verifyFast`, run runtime and GameTest validation with `./gradlew verifyFull`, and stage the reobfuscated jar with `./gradlew stageRuntimeJar`.

## Cold source native fixture

`verifyFull` binds a fresh `build/gametest/<UUID>` to the actual Forge RunConfig, preparation,
structures and audited sink. CREATE_NEW settings preserve existing bytes; no old log/world or
ordinary/visual run is deleted or adopted. Exactly six completed/passed native tests and strict
latest/debug ERROR/FATAL/mixin-failure checks are required. Pure creation/preservation and
missing/count-mismatch/duplicate/severity controls run through `verifyColdNativeFixtureGuard`.
Retain complete prior build outputs before `clean`. Historical warm-fixture passes do not prove
this new cold profile; actual admitted full/stage verification remains required.

## Bounded playtest diagnostics

Loader-provided SLF4J categories under `bc.playtest.better_block_placement_preview` report startup
at INFO and advisory reconstruction/rejection/verdict, request submission and accepted/stale cache
responses at DEBUG. A verdict is not an actual placement or physics execution. Sequences identify
local requests, not a globally unique cross-player operation; no packet fields change. Tint/render
queries remain silent. Client disconnect discards pending/advisory cache state without rewinding
sequence numbers.

One mod-wide bucket shares 120 DEBUG/TRACE records per minute, burst 20, at most 256 aggregation
keys, 2048-character records and 30-second summaries. Teardown clears aggregate keys but does not
refill the bucket. Standalone stays INFO; the pack selects scoped DEBUG. Focused server TRACE
requires `bc.playtest.trace.better_block_placement_preview.dimension` and exactly one of `.subject`
(canonical UUID) or `.pos` (`x,y,z` block coordinates). Only matching requests arm the one-process
60-second/1000-record session, still under the ordinary bucket. Focused records may include the
causal requesting player UUID and clicked position; default DEBUG does not. No item tags, names,
payloads, new world/chunk reads or gameplay RNG are used for diagnostics.
