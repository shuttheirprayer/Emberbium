# Emberbium — project status

Last updated 2026-10-16.

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
| `3e81eb5a` | Video settings GUI animation pass. Hover, selection, tick boxes, the slider reveal, scrolling and tab switching are eased instead of snapping. Render-layer only -- the addon API is data-level (`OptionPage`/`OptionGroup`/`Option`), and no existing signature changed. |
| `431aa5b1` | `RenderRegionManager` split each region's build results with two stream filters, and `GlBufferArena` collected the streamed uploads into a `LinkedList`. Both are plain loops now; the arena gained a `LinkedList` overload and its `Stream` overload delegates to it. |
| `b438754b` | `ClonedChunkSectionCache.cleanup()` walked every entry with `removeIf`. The map's link order is also timestamp order, so it now pops expired entries from the front. |
| `bbb102c6` | Animated sprites that are not visible still advance a frame counter every tick. The frame times are copied into an `int[]` once instead of read through two accessors per tick. |
| `bbe52d0e` | `VertexSorters` switches to an LSD radix sort from 256 keys. Same permutation as the merge sort (`checks/RadixSortCheck`), 3-7x faster. |
| `7550d4e7`, `266d9d3b` | Touching facing slices of a section are drawn as one multidraw command, with or without a shader pack: 703 to 803 fps shaders off, 105.5 to 108.8 fps with Complementary. See Performance below. |
| `c949df64` | `ChunkMeshBufferBuilder.grow()` multiplied by the vertex stride twice, so a facing that overflowed its scratch buffer reallocated to 100 MiB (400 MiB with Emberlens' 40-byte vertices) instead of 5 (10). Sodium fixed the same bug in 0.5.12 and 0.6. |
| `36237a77` | Fix for `aa933934`: the colour memo is also keyed on the quad's sprite. COO's Copycats+ bridge sets the colour property from the sprite, so a multi-material copycat gave one part's colour to another. |
| `0f1ea0f2` | Mesh builds write the facings visible from the build-time camera first, so one command covers them, as Sodium 0.7 does: 1,558 to 980 terrain commands per frame, 824 to 834 fps shaders off. |
| `739ea6f5` | With a shader pack loaded, opaque terrain is drawn in four bands of camera distance, SOLID then CUTOUT in each, so the early depth test rejects more: 108.3 to 111.4 fps. Off without shaders, where it measured nothing. |

Non-behaviour: the rebrand (`fdfa0a6a`), the vendored ModDevGradle plugin (`714ea9ef`), the
version anchor (`42a1846f`).

`checks/` holds standalone assertion checks for logic that a real instance is needed to
exercise. Both are plain `javac`, no framework:

```
javac checks/DrainCheck.java -d /tmp && java -ea -cp /tmp DrainCheck
javac checks/CoplanarCheck.java -d /tmp && java -ea -cp /tmp CoplanarCheck
javac checks/TintMemoCheck.java -d /tmp && java -ea -cp /tmp TintMemoCheck
javac checks/AnimationCheck.java -d /tmp && java -ea -cp /tmp AnimationCheck
javac -d /tmp/rs src/main/java/me/jellysquid/mods/sodium/client/util/sorting/{AbstractSort,InsertionSort,MergeSort,RadixSort}.java checks/RadixSortCheck.java && java -ea -cp /tmp/rs RadixSortCheck
javac checks/MeshGrowthCheck.java -d /tmp && java -ea -cp /tmp MeshGrowthCheck
javac checks/FacingOrderCheck.java -d /tmp && java -ea -cp /tmp FacingOrderCheck
```

`DrainCheck` covers the drain logic in `d9018065`; `CoplanarCheck` the length handling and the
allocation decision in `201967f2`; `TintMemoCheck` the memo invariants in `aa933934`, including the sprite key;
`AnimationCheck` the easing maths behind the GUI animations; `RadixSortCheck` compiles the real
sort sources and requires the radix sort to return the merge sort's permutation exactly;
`MeshGrowthCheck` the vertex-count units of `ChunkMeshBufferBuilder.grow()`; `FacingOrderCheck`
the visible-facings-first layout, its merged draws and its rebase after an arena move.

## Performance (2026-10-16)

Measured in the Linggango Developer Version pack on an RX 9070 XT, 3440x1440, render distance
12, at a fixed spot in a dense taiga, with an in-game A/B harness that switches one code path
every second and times frames, terrain CPU and per-pass GPU time with timestamp queries. The
harness is the local branch `perf-bench`; it is never merged.

What the numbers say about this card's GL driver:

- Each multidraw command costs about 68 ns of GPU time regardless of size. Without shaders that
  makes the command count the main terrain cost, which is why merging and the visible-first
  layout pay off there.
- With Complementary the frame is about 8.6 ms of GPU time, 5.8 ms of it the main view's opaque
  terrain and only ~0.13 ms the shadow pass. It is pixel shading, not vertices: switching block
  face culling off added only ~0.2 ms. Drawing the same terrain far-to-near cost 2.8 ms more, so
  draw order is the lever there.

Measured and rejected:

| Experiment | Result |
|---|---|
| `glMultiDrawElementsIndirect` instead of the direct multidraw | +0.05 ms GPU, 842 to 809 fps. Slower on this driver. |
| CUTOUT pass before SOLID | +0.12 ms GPU |
| SOLID and CUTOUT interleaved per region | half the gain of the distance bands, for the same CPU |
| Distance bands without a shader pack | no measurable change, +0.03 ms CPU |

The "shaders went from ~350 to ~100 fps" report was scenery, not code: Emberlens 1.8.3 (the
build that ran on 2026-10-05) and 1.8.4 measure the same at the fixed spot, the Oculus and
Complementary settings match the 2026-10-05 snapshot, and the same spot runs at 264 fps looking
at the ground instead of into the forest.

## Video settings GUI

Animated at the render layer only. What moves:

| Where | Motion |
|---|---|
| `FlatButtonWidget` | hover background cross-fade; enabled/disabled cross-fade; the selection marker grows out of the middle of its edge |
| `ControlElement` | hover background cross-fade, and the label slides 2px in from the edge |
| `TickBoxControl` | the fill scales out of the centre while the border takes on the accent colour |
| `SliderControl` | the bare value and the full slider cross-fade instead of swapping; the track wipes out from the thumb |
| `CyclingControl` | a changed value arrives dimmed and slightly out from the edge, then settles into place |
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
- Text is never faded below `Animation.MIN_TEXT_ALPHA` (4/255). Minecraft's `Font.adjustColor` is
  `(color & 0xFC000000) == 0 ? color | 0xFF000000 : color`, so text at alpha 0-3 comes out **fully
  opaque** -- a fade that runs to zero flashes the text solid on its last frame instead of
  disappearing. Fills have no such fixup and can fade to zero normally.
- Selecting the tab that is already open does not replay the slide.

The accent colour for selection markers, tick boxes, the option-group highlight and the logo
watermark is the single constant `DefaultColors.ELEMENT_ACTIVATED`, now `0xFF679BF5`.

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
