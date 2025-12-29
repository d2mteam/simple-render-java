# Simple Render (Java)

This project is a minimal voxel renderer skeleton built in Java. It is organized to demonstrate the split between **Object-Oriented Design (OOP)** for modeling the world and **Data-Oriented Design (DOD)** for fast render processing.

## Architecture Overview

```
app/
scene/
camera/
world/
render/
  pipeline/
  mesh/
  buffer/
  culling/
gl/
math/
asset/
```

### OOP Zone (What exists)
Located in `app/`, `scene/`, and `camera/`.

* `GameApplication`, `GameLoop`, `EngineConfig`, `Time`
* `Scene`, `RenderableChunk`
* `Camera`, `CameraController`

These classes model the state of the engine and world. They should be readable and expressive.

### World Snapshot Boundary
Located in `world/`.

* `ChunkMeshData`
* `ChunkBlockView`

Render code consumes **snapshots** only. It should never depend on domain objects.

### DOD Zone (How data flows)
Located in `render/`.

* `ChunkMesher`, `MeshData`, `MeshCache`
* `FrustumCuller`
* `RenderPipeline`

These classes are designed for flat loops and batched data processing.

### GPU / OpenGL Zone
Located in `gl/`.

* `OpenGLRenderer`
* `ShaderProgram`, `VertexArray`, `VertexBuffer`, `IndexBuffer`, `GPUMesh`

All OpenGL/LWJGL access must be contained here.

## Data Flow

```
World -> ChunkMeshData -> ChunkMesher -> MeshData -> GPUMesh -> OpenGL
```

## Render Loop Rules

* The game loop calls `update()` and `render()` in order.
* No OpenGL usage outside `gl/`.
* Avoid allocations inside `render()`.

## Status

This is an educational skeleton. Mesh generation, chunk updates, and LWJGL bindings are placeholders.
