# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew lwjgl3:run    # Run desktop app
./gradlew lwjgl3:jar    # Build runnable JAR
./gradlew teavm:run     # Serve web app at localhost:8080
./gradlew build         # Build all modules
./gradlew clean         # Clean build
```

No automated tests exist in this project.

## Architecture

**The Depths Below** is a 3D submarine exploration game built with libGDX + Kotlin.

### Module structure
- `core/` — all game logic; platform-independent
- `lwjgl3/` — desktop launcher (1920×1080)
- `teavm/` — experimental web/JS transpilation via TeaVM
- `gdx-lava/` — git submodule providing base `MainGame`/`KtxGame` wrappers and a story/narrative system (`turbofacts`)

### Core game stack
- **libGDX 1.11.0** — rendering, input, asset management
- **Ashley 1.7.4** — Entity Component System (ECS); all gameplay logic lives in systems under `depth/ecs/systems/`
- **Bullet Physics** — 3D physics
- **GLTF Scene3D** — 3D model loading and scene rendering
- **KTX** — Kotlin extensions for libGDX; used throughout

### Key entry points
- `depth/core/TheDepthsBelow.kt` — main game class, sets up screens
- `depth/core/GameScreen.kt` — primary game screen, wires ECS engine
- `depth/injection/Context.kt` — KTX dependency injection container; systems and managers are registered here

### Terrain / voxel system
Procedural underwater terrain uses marching cubes:
- `depth/voxel/` — `Block`, `Chunk`, `Terrain`, `BlockManager`, `MapGenerator`
- `depth/marching/` — `MarchingCubeBuilder` (mesh generation), `WorldManager` (chunk lifecycle)

### ECS layout
All Ashley systems are in `depth/ecs/systems/`. Notable ones:
- `RenderSystem3d` — GLTF scene rendering
- `SubmarineControlSystem` — player input → physics forces
- `BulletUpdateSystem` — steps the Bullet physics world

Components are in `depth/ecs/components/` (15+ types).
