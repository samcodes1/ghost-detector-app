# 3D Model

A single CC-BY ghost figure is bundled with Ghost Finder:

| File           | Size  | Source                                                                                                                                | Author                                            | License                                                            |
|----------------|-------|---------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|--------------------------------------------------------------------|
| `assassin.glb` | 4.9 MB | [Dark Faceless Black Assassin](https://sketchfab.com/3d-models/dark-faceless-black-assassin-95ac882f1d504dc790d52aa19713ff3b)         | [Pigcraft](https://sketchfab.com/s8819296)        | [CC-BY 4.0](https://creativecommons.org/licenses/by/4.0/)         |

The original glTF + textures + 60 MB binary were Draco-compressed into one
self-contained GLB. The visual is unchanged; only the on-disk size dropped
~92 %. Filament (which SceneView uses) decompresses Draco natively.

### Credit (required by CC-BY)
> This work is based on "Dark Faceless Black Assassin"
> (<https://sketchfab.com/3d-models/dark-faceless-black-assassin-95ac882f1d504dc790d52aa19713ff3b>)
> by Pigcraft (<https://sketchfab.com/s8819296>) licensed under CC-BY-4.0.

### How `GhostManager` uses it
- Detects a **doorway** = a vertical AR plane whose bottom edge sits on a
  horizontal floor plane, within ~5 m and roughly in the user's field of view.
- Spawns the assassin standing at that doorway, facing into the room.
- **Idles ~3.8 s** with a faint sway.
- **Glides away** along the floor (X/Z only, Y locked to floor) for ~5.5 s with
  smoothstep easing and a subtle vertical bob.
- **Fades out** by scaling to zero over ~1.4 s — material alpha is hard to
  blend on the fly so we dissipate via scale.
- 12 s cooldown before another doorway can spawn.

If you swap the file, keep the name `assassin.glb` and the code will pick it
up automatically.
