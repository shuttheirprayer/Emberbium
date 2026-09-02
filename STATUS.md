# Emberbium — project status

Last updated 2026-09-03.

## What this is

A fork of [Embeddium](https://github.com/embeddedt/embeddium) `20.1/forge`, based at commit
`dee0ebd` (tagged `upstream-base-dee0ebd`). Minecraft 1.20.1, Forge.

The Forge mod id stays **`embeddium`** permanently. Only the display name, log output and
config screens say "Emberbium". See `README.md` for why.

## Repository

| | |
|---|---|
| Base | `upstream-base-dee0ebd` = upstream `dee0ebd` |
| Remote `upstream` | FiniteReality/embeddium — same repo as embeddedt/embeddium |
| Remote `xenon` | txnimc/xenon — reference only, never merged |
| Remote `aunaturel` | ACowAdonis/embeddium-au-naturel — reference only, unrelated history |

Build: `./gradlew build`, JDK 17, output in `build/libs/`. Use the plain jar, not `-slim`
(that one is pre-reobfuscation and will not work in a real instance). `./gradlew
runGameTestCiClient` runs the two automated render tests and exits.

`mod_version` and `base_mc_version_commit` in `gradle.properties` must stay on upstream's
version line — addons declare Forge dependency ranges against the `embeddium` id, and a
fork-flavoured version sorts below all of them. The reasoning is written above the anchor.

## Changes from upstream

Behaviour:

| Commit | Change |
|---|---|
| `7cbb5351` | Removed the `FlawlessFramesService` ServiceLoader entry, which class-loaded `TestRegistry` during normal play and broke joining servers without Embeddium. Cherry-picked from au-naturel. |
| `5729de2f` | `GlFence.sync(long)` used `glWaitSync`, which blocks the GPU not the CPU and violates the spec by passing a timeout. Now `glClientWaitSync` with result checking. Latent — nothing calls `sync()`. |
| `d9018065` | `collectChunkBuildResults()` aborted its drain at the first failed chunk build, stranding every output already polled. In `destroy()` it also skipped GL and off-heap teardown entirely, leaking region storage that has no reclaimer behind it. |
| `7ceb626f` | Ported DSA / modern GL buffer paths from au-naturel behind `advanced.useModernGlPaths`, default on. **Never benchmarked.** |
| `024cab76` | Reverted two changes that were made on a wrong diagnosis. |

Non-behaviour: the rebrand (`fdfa0a6a`), the vendored ModDevGradle plugin (`714ea9ef`), the
version anchor (`42a1846f`).

`checks/DrainCheck.java` covers the drain logic in `d9018065`:

```
javac checks/DrainCheck.java -d /tmp && java -ea -cp /tmp DrainCheck
```

## Open: Complementary shader artifacts

Parked by request. Large translucent right-triangle wedges on distant terrain, grid-aligned,
fading into fog. **Only** with ComplementaryReimagined_r5.8.1 — Mellow and Solas render fine.
Reported as not happening on stock `embeddium-0.3.31`.

Ruled out by testing:

- The DSA vertex attribute path — artifacts persisted with `vertexAttribute=LEGACY` in the log.
- Buffer DSA — artifacts persisted with `useModernGlPaths` off entirely.
- `use_render_pass_optimization` — `BlockRenderer.java:100` already gates it on
  `!ShaderModBridge.areShadersEnabled()`.
- `CloudRenderer` — uses a vanilla `BufferBuilder`, not `GlTessellation`. `DefaultChunkRenderer`
  is the only `createTessellation` call site.

**The one test never run:** build from `upstream-base-dee0ebd` and load that with Complementary.
Artifacts there means an upstream regression among the 73 commits since 0.3.31 and nothing to do
with this fork; artifacts absent means it is in the four changes above.

A local repro is not available. The dev environment uses official mappings and Oculus ships an
SRG refmap, so it dies at `@Shadow field f_117950_ was not located in the target class
AbstractTexture` before the window opens. Reproducing needs a real production instance.

Separately: Complementary r5.8.1 asks Iris for `BIOME_PALE_GARDEN` (a Minecraft 1.21.4 biome)
and `endFlashIntensity`, and both fail to resolve on Oculus 1.8.0.1 — that pack is newer than
this Iris supports. Those uniforms drive biome tint and End flash, not geometry, so it is a real
version mismatch but not established as the cause.

## Next

1. Finish the `SectionRenderDataUnsafe` retention audit. The `destroy()` hole is fixed; confirm
   no other path drops a `RenderRegion` without `delete()`.
2. Translucency sorting retention — `SortState` keeps a `float[]` per dynamic-sort section and
   `compactForStorage()` only nulls it for `STATIC`. Check `RenderSection.delete()` clears them.
   Also `getSortState()` allocates on the meshing path via `toArray(new float[0])`.
3. Benchmark the DSA port with spark or delete it. It has been on by default and unmeasured.
4. Report the stale `mixin.env.refMapRemappingFile` path upstream — it points at a ForgeGradle
   path ModDevGradle never creates, left over from the MDG migration in `151d121e`.

Declined deliberately: merging Xenon's `extras` or options GUI (duplicates Embeddium Extra and
TexTrue's, which this fork must stay compatible with), bulk-adopting au-naturel (squashed
history, no common ancestor, 12 of 23 commits conflict), and Xenon's aggressive leaf culling
(skips the vertical neighbour check, which is why it leaves holes in canopies).
