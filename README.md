# Better Block Placement Preview

Adds an advisory, server-authoritative RBP support prediction to RotaVision block placement ghosts. Teal marks a supported proposal, amber marks a predicted fall or crush, and white marks an unavailable prediction. Vanilla-invalid placement remains red; the preview never places blocks or runs RBP physics.

Requires Forge 1.20.1, RotaVision 1.0.2, Realistic Block Physics 1.0.0, and Realistic Physics 1.0.1.

Build with `./gradlew verifyFast`, run runtime and GameTest validation with `./gradlew verifyFull`, and stage the reobfuscated jar with `./gradlew stageRuntimeJar`.
