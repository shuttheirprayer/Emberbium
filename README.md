<img src="src/main/resources/icon.png" width="128">

# Emberbium

A performance-focused fork of [Embeddium](https://github.com/embeddedt/embeddium), which is itself a Forge port of
[Sodium](https://github.com/CaffeineMC/sodium-fabric).

**This fork is not supported by embeddedt or CaffeineMC.** Do not report issues encountered with Emberbium to their
bug trackers or Discord servers. Report them here instead.

## Mod id

Emberbium registers under the mod id `embeddium`, deliberately and permanently. The entire addon ecosystem
(Oculus, Embeddium Extra, FerriteCore, TexTrue's Embeddium Options and others) resolves that id and links against
the `org.embeddedt.embeddium` / `me.jellysquid.mods.sodium` packages. Renaming it would break every one of them,
and the failure would be silent rather than loud. Only the display name, log output and configuration screens say
"Emberbium".

Existing `embeddium-options.json` and `embeddium-mixins.properties` config files are picked up unchanged.

## Goals

* Track upstream Embeddium `20.1/forge` closely; carry as little divergence as possible
* Audit the off-heap allocation paths, translucency sorting retention and chunk build lifecycle for leaks
* Keep the chunk meshing and entity rendering loops allocation-free
* Preserve backwards compatibility for downstream addons

## Building

Requires JDK 17.

```
./gradlew build
```

The jar is written to `build/libs/`. Use `-Pbuild.release` for a clean version string without the beta counter.

## Credits

* **JellySquid & the CaffeineMC team** - for Sodium, the foundation of all of this
* **embeddedt** - for creating and maintaining Embeddium
* **Asek3** - for the original Forge port (Rubidium)
* **NanoLive** - for Magnesium, which Rubidium descends from

## License

Emberbium is licensed under the GNU LGPL v3, the same license as Embeddium and Sodium. See [COPYING](COPYING) and
[COPYING.LESSER](COPYING.LESSER).
