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
| `aa933934` | `BlockRenderer.getVertexColors()` asked the colour provider once per quad, though every quad in a `renderModel()` call shares the same world, position and state. Now memoises the last (tint index, colour) per block. Adapted from au-naturel `834e28e6`, without its per-section hash map. |
| `PENDING` | Video settings GUI animation pass. Hover, selection, tick boxes, the slider reveal, scrolling and tab switching are eased instead of snapping. Render-layer only -- the addon API is data-level (`OptionPage`/`OptionGroup`/`Option`), and no existing signature changed. |

Non-behaviour: the rebrand (`fdfa0a6a`), the vendored ModDevGradle plugin (`714ea9ef`), the
version anchor (`42a1846f`).

`checks/` holds standalone assertion checks for logic that a real instance is needed to
exercise. Both are plain `javac`, no framework:

```
javac checks/DrainCheck.java -d /tmp && java -ea -cp /tmp DrainCheck
javac checks/CoplanarCheck.java -d /tmp && java -ea -cp /tmp CoplanarCheck
javac checks/TintMemoCheck.java -d /tmp && java -ea -cp /tmp TintMemoCheck
javac checks/AnimationCheck.java -d /tmp && java -ea -cp /tmp AnimationCheck
```

`DrainCheck` covers the drain logic in `d9018065`; `CoplanarCheck` the length handling and the
allocation decision in `201967f2`; `TintMemoCheck` the memo invariants in `aa933934`;
`AnimationCheck` the easing maths behind the GUI animations.

## Video settings GUI

Animated at the render layer only. What moves:

| Where | Motion |
|---|---|
| `FlatButtonWidget` | hover background cross-fade; the selection marker grows out of the middle of its edge |
| `ControlElement` | hover background cross-fade, and the label slides 2px in from the edge |
| `TickBoxControl` | the fill scales out of the centre while the border takes on the accent colour |
| `SliderControl` | the bare value and the full slider cross-fade instead of swapping; the track wipes out from the thumb |
| `ScrollBarComponent` | eased scroll position, thumb highlight on hover and drag |
| `TabFrame` | a newly selected page settles in from the right |

`org.embeddedt.embeddium.gui.animation.Animation` is the whole mechanism: one eased value plus
colour helpers. It uses exponential smoothing rather than start/duration tweens because every one
of these states can reverse mid-flight -- the cursor leaves a button before its fade finished --
and smoothing handles that with nothing to restart.

**Why this cannot reach addons.** The addon-facing API is data-level: `OptionGUIConstructionEvent`,
`OptionPageConstructionEvent` and `OptionGroupConstructionEvent` all trade in `OptionPage`,
`OptionGroup` and `Option`, and the screen builds every widget itself. Addons supply no widgets,
renderers or screens. Every edit here adds members and leaves existing signatures alone, so a
custom `Control` implementation still works, and a custom `ControlElement` subclass inherits the
animation if it calls `super.render()` and simply does not animate if it does not.

Two rules the code follows, both of which are correctness rather than polish:

- Anything that scrolls is drawn *and* hit-tested from the eased offset (`getRenderOffset()`), never
  the target. Using the target for one and the eased value for the other puts the cursor out of step
  with the screen while a scroll settles, and clicks land on the wrong row. `TabFrame`'s slide shifts
  the cursor by the same amount for the same reason.
- Dragging a scrollbar thumb snaps rather than eases. A drag is direct manipulation; easing it feels
  like the content is on elastic.

**Not verified visually.** There is no way to see the screen from here -- `runGameTestCiClient`
never opens it, and the checks cover the maths, not the look. Timings (45-70ms half-lives, a 10px
tab slide) are a starting point and are meant to be argued with.

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
Artifacts there means an upstream regression between 0.3.31 and this base, and nothing to do with
this fork; artifacts absent means it is in the four changes above.

That search space is **19 commits** (`87d1a75a..upstream-base-dee0ebd`), not the 73 recorded here
earlier -- 73 is the distance from `0263cc2f`, which is 0.3.24 and what `base_mc_version_commit`
points at, not the published release. Of the 19, twelve touch no runtime code at all (build system,
CI, docs, and a move-then-revert pair of the mixin sourceset that nets to zero). The candidates for
a geometry artifact are `170a1c1a` (routes a quad to a different render pass based on its texture),
`496c59e4` plus its follow-up `28ebce18` (defers BakedQuad flag and normal computation), and
`48950030` (implements the getQuads extension on SimpleBakedModel).

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

## Surveyed: the other forks

Read 2026-09-03 from the `xenon` and `aunaturel` remotes. Nothing further is worth taking; do not
redo this survey without a reason.

**Xenon (`xenon/20.1/forge`)** — last commit 2024-09-24, merge base `496c59e4`, far behind this
base. Eight non-merge commits: the rebrand, package renames, its own options GUI (`Dim2i`,
`FlatButtonWidget`), and the `extras` package — leaf culling, frametime graph, entity distance
culling. All already declined. Its only renderer edits are the hooks for the aggressive leaf
culling. It also "fixed" the gametest class-loading problem by commenting the registration out,
where `7cbb5351` removes the ServiceLoader entry properly.

**Au Naturel (`aunaturel/main`)** — alive, same `0.3.31` base anchor. 24 commits, 66 files differ
(+1867/-490), but only six files are new, so it is mostly deep edits to shared files, and those
edits are one interlocking body of work: multi-pass terrain visibility, a rewritten `GlBufferArena`
with a buffer pool and 64-bit offsets, and camera-weighted chunk build prioritisation.

The trap is that its recent commits read like cherry-pickable bugfixes and are not — they fix its
own code:

| Commit | Fixes | Applies here |
|---|---|---|
| `3b8fff87` "nighttime lighting too bright" | an `encodeLight` offset its own `7b1dec30` added | No — `CompactChunkVertex` already writes `vertex.light` |
| `9fec9cbe` "section-vs-block coordinate mismatch" | its own `ChunkJobPriority` | No — no such class here |
| `1ec3cc6a` "arena pool lifecycle" | the buffer pool its own `7b1dec30` added | No — this `GlBufferArena` has no pool |

Taken: the colour cache idea, in simplified form, at `aa933934`.

Considered and not taken:

- `7c2a3ec2` configurable block-entity render distance. Self-contained, but duplicates Embeddium
  Extra and TexTrue's, which is why Xenon's entity culling was declined.
- `56b6c78f` pre-sizing two staging queues. Two characters, no measurable payoff after the first
  frames.
- The `DefaultChunkRenderer` half of `e3ec533a`, a long index-pointer mask. Does not port: its
  `0xFFFFFFFFFFFFFFFFL` assumes au-naturel's 64-bit offsets, and against this tree's `int` offsets
  it still sign-extends. The correct literal here is `0xFFFFFFFFL`, and it is unreachable without a
  2 GB index arena in one region.

`upstream/20.1/forge` is still at `dee0ebd`, this fork's base, so there is nothing to pull from
upstream either.

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
