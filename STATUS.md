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
| `7ceb626f` | Ported DSA / modern GL buffer paths from au-naturel behind `advanced.useModernGlPaths`. Default off as of `000b856d`. **Never benchmarked.** |
| `024cab76` | Reverted two changes that were made on a wrong diagnosis. |
| `201967f2` | `TranslucentQuadAnalyzer.getSortState()` copied the quad centers before deciding whether the section needed them, allocating a `float[]` per translucent section per rebuild on the meshing path even for the `NONE` level. The coplanarity test now reads the list's backing array in place. |
| `000b856d` | `advanced.useModernGlPaths` now defaults to off, and the tooltip says the option is experimental and what it does at each GL level. The port stays in the tree for testing on modern hardware. No serialized key rename, so a config that already has `use_modern_gl_paths=true` keeps it on. |

Non-behaviour: the rebrand (`fdfa0a6a`), the vendored ModDevGradle plugin (`714ea9ef`), the
version anchor (`42a1846f`).

`checks/` holds standalone assertion checks for logic that a real instance is needed to
exercise. Both are plain `javac`, no framework:

```
javac checks/DrainCheck.java -d /tmp && java -ea -cp /tmp DrainCheck
javac checks/CoplanarCheck.java -d /tmp && java -ea -cp /tmp CoplanarCheck
```

`DrainCheck` covers the drain logic in `d9018065`; `CoplanarCheck` covers the length handling
and the allocation decision in `201967f2`.

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

## Closed

### `SectionRenderDataUnsafe` retention audit — no leak found (2026-09-03)

Static reachability audit, not a runtime measurement. `allocateHeap` has exactly one caller
(`SectionRenderDataStorage`'s constructor) and `freeHeap` exactly one (`SectionRenderDataStorage.delete()`),
which only `RenderRegion.delete()` calls, so the whole question reduces to: can a `RenderRegion`
become unreachable without `delete()`?

Regions live only in `RenderRegionManager.regions`. Every way an entry leaves that map:

- `create()` — `put` only when `get(key) == null`, so it never overwrites a live region.
- `update()` — `delete(commandList)` immediately before `it.remove()`.
- `delete()` — deletes every region before `clear()`.
- the manager itself being dropped — its only owner is `RenderSectionManager.regions` (final), and
  `RenderSectionManager` is only dropped at the two `SodiumWorldRenderer` sites
  (`unloadWorld`, `initRenderer`), both of which call `destroy()` first. The `SodiumWorldRenderer`
  itself is created in the `LevelRenderer` constructor and never replaced.

Two supporting invariants that make `update()` safe:

- `isEmpty()` is `sectionCount == 0`, and a non-disposed `RenderSection` is always registered in its
  region, so a region holding a live section is never deleted. `filterChunkBuildResults()` drops
  disposed sections, so `uploadMeshes` cannot resurrect storage on a deleted region.
- After deletion, `sectionRenderData` is cleared, and `DefaultChunkRenderer.render()` skips a region
  whose `getStorage(pass)` is null. A stale `ChunkRenderList` left in `renderLists` for a region
  deleted mid-frame reads nothing from the freed pointer.

Two latent traps noticed while auditing, neither reachable today, neither patched:

- `MultiDrawBatch.delete()` frees three aligned allocations without nulling the pointers, and
  `ShaderChunkRenderer.delete()` does not clear `programs`. A second `destroy()` would double-free.
  It can only be reached if `destroy()` throws — `collectChunkBuildResults()` still rethrows a failed
  build — because `unloadWorld` then skips `renderSectionManager = null`. That exception is a
  `ReportedException` that crashes the game before a second `destroy()` can run.
- `ChunkBuilderSortTask.execute()` calls `sortBuffer.centers().length` for every entry of the sort
  state map, but `compactForStorage()` nulls `centers` for `STATIC`. `createSortTask` only checks
  that *some* entry is dynamic. Safe only because `TRANSLUCENT` is the sole `isSorted()` pass, so the
  map never holds more than one entry.

### Translucency sorting retention — one allocation removed, no leak (2026-09-03)

`RenderSection.delete()` does not clear `translucencySortStates`, and does not need to. By the time
it runs, `onSectionRemoved` has already dropped the section from `sectionByPosition` and nulled
`region.sections[index]`, so the section and its `float[]` are unreachable and the GC takes them.
The only place a disposed section lingers is a `rebuildLists` queue, which `submitRebuildTasks`
drains and discards every frame.

The `float[]` a `DYNAMIC` state keeps is not retention either — it is the input to every later
resort. `compactForStorage()` correctly drops it for `STATIC`, whose sort already happened during
meshing.

What was real is the allocation, fixed at `201967f2`. See the commit message.

## Next

1. Report the stale `mixin.env.refMapRemappingFile` path upstream — it points at a ForgeGradle
   path ModDevGradle never creates, left over from the MDG migration in `151d121e`.

Still unmeasured, now without a deadline: the DSA port's actual effect on frame time. It is off by
default, so it costs stock users nothing, but nobody has benchmarked it. If it is ever to be
defaulted on, that needs spark numbers first -- fixed position, `doDaylightCycle false`,
`time set noon`, `weather clear`, `/spark profiler --thread "Render thread" --timeout 60`, same
settings both runs.

Declined deliberately: merging Xenon's `extras` or options GUI (duplicates Embeddium Extra and
TexTrue's, which this fork must stay compatible with), bulk-adopting au-naturel (squashed
history, no common ancestor, 12 of 23 commits conflict), and Xenon's aggressive leaf culling
(skips the vertical neighbour check, which is why it leaves holes in canopies).
