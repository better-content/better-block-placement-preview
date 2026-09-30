# AGENTS.md

## Scope

This repository contains the Better Content Forge mod **Better Block Placement Preview**.

- Canonical mod ID: `better_block_placement_preview`
- Canonical artifact: `better-block-placement-preview-<version>.jar`
- Maven group: `com.bettercontent`
- Java runtime: 17
- Minecraft/Forge baseline: 1.20.1 / 47.4.13

## Commit discipline

Commit after each coherent completed change. Run documented validation before committing and push the current branch.

## Validation

Run `./gradlew verifyFast` for deterministic checks. Run `./gradlew verifyFull` for runtime or GameTest changes. Stage deployable runtime artifacts with `./gradlew stageRuntimeJar`.

Do not commit build outputs, runtime worlds, logs, IDE state, or downloaded dependency JARs.
