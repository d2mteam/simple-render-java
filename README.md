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

## Lịch sử thay đổi (từ commit đầu tiên đến hiện tại)

- `2266448` first
  - Files:
    - `.gitignore`
    - `.idea/.gitignore`
    - `.idea/gradle.xml`
    - `.idea/misc.xml`
    - `build.gradle`
    - `gradle/wrapper/gradle-wrapper.jar`
    - `gradle/wrapper/gradle-wrapper.properties`
    - `gradlew`
    - `gradlew.bat`
    - `settings.gradle`
    - `src/main/java/com/Main.java`

- `088262f` Add voxel renderer architecture skeleton
  - Files:
    - `README.md`
    - `src/main/java/com/Main.java`
    - `src/main/java/com/simplerender/Main.java`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/Time.java`
    - `src/main/java/com/simplerender/camera/Camera.java`
    - `src/main/java/com/simplerender/camera/CameraController.java`
    - `src/main/java/com/simplerender/camera/CameraSnapshot.java`
    - `src/main/java/com/simplerender/gl/GPUMesh.java`
    - `src/main/java/com/simplerender/gl/IndexBuffer.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/gl/VertexArray.java`
    - `src/main/java/com/simplerender/gl/VertexBuffer.java`
    - `src/main/java/com/simplerender/math/Vector3f.java`
    - `src/main/java/com/simplerender/render/buffer/RenderBuffer.java`
    - `src/main/java/com/simplerender/render/culling/FrustumCuller.java`
    - `src/main/java/com/simplerender/render/mesh/ChunkMesher.java`
    - `src/main/java/com/simplerender/render/mesh/MeshCache.java`
    - `src/main/java/com/simplerender/render/mesh/MeshData.java`
    - `src/main/java/com/simplerender/render/pipeline/RenderPipeline.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/java/com/simplerender/scene/SceneSnapshot.java`
    - `src/main/java/com/simplerender/world/ChunkBlockView.java`
    - `src/main/java/com/simplerender/world/ChunkMeshData.java`

- `a6ecf0b` Add random chunk snapshots to scene
  - Files:
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/java/com/simplerender/scene/SceneSnapshot.java`
    - `src/main/java/com/simplerender/world/ChunkMeshDataFactory.java`

- `5de6d65` Restrict GPU resource access to renderer
  - Files:
    - `src/main/java/com/simplerender/gl/GPUMesh.java`
    - `src/main/java/com/simplerender/gl/IndexBuffer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/gl/VertexArray.java`
    - `src/main/java/com/simplerender/gl/VertexBuffer.java`

- `233fd6e` Merge pull request #1 from d2mteam/codex/create-standalone-voxel-render-engine
  - Files: (không có thay đổi trực tiếp trong commit này)

- `f01f304` Implement runnable logging renderer example
  - Files:
    - `README.md`
    - `build.gradle`
    - `src/main/java/com/Main.java`
    - `src/main/java/com/simplerender/Main.java`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/Time.java`
    - `src/main/java/com/simplerender/camera/Camera.java`
    - `src/main/java/com/simplerender/camera/CameraController.java`
    - `src/main/java/com/simplerender/camera/CameraSnapshot.java`
    - `src/main/java/com/simplerender/gl/GPUMesh.java`
    - `src/main/java/com/simplerender/gl/IndexBuffer.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/gl/VertexArray.java`
    - `src/main/java/com/simplerender/gl/VertexBuffer.java`
    - `src/main/java/com/simplerender/math/Vector3f.java`
    - `src/main/java/com/simplerender/render/buffer/RenderBuffer.java`
    - `src/main/java/com/simplerender/render/culling/FrustumCuller.java`
    - `src/main/java/com/simplerender/render/mesh/ChunkMesher.java`
    - `src/main/java/com/simplerender/render/mesh/MeshCache.java`
    - `src/main/java/com/simplerender/render/mesh/MeshData.java`
    - `src/main/java/com/simplerender/render/pipeline/RenderPipeline.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/java/com/simplerender/scene/SceneSnapshot.java`
    - `src/main/java/com/simplerender/world/ChunkBlockView.java`
    - `src/main/java/com/simplerender/world/ChunkMeshData.java`
    - `src/main/java/com/simplerender/world/ChunkMeshDataFactory.java`

- `09c8e08` Merge branch 'master' into codex/create-standalone-voxel-render-engine-cbjg96
  - Files: (không có thay đổi trực tiếp trong commit này)

- `7096a53` Merge pull request #2 from d2mteam/codex/create-standalone-voxel-render-engine-cbjg96
  - Files: (không có thay đổi trực tiếp trong commit này)

- `7a360b3` Implement LWJGL OpenGL rendering demo
  - Files:
    - `README.md`
    - `build.gradle`
    - `src/main/java/com/Main.java`
    - `src/main/java/com/simplerender/Main.java`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/Time.java`
    - `src/main/java/com/simplerender/camera/Camera.java`
    - `src/main/java/com/simplerender/camera/CameraController.java`
    - `src/main/java/com/simplerender/camera/CameraSnapshot.java`
    - `src/main/java/com/simplerender/gl/GPUMesh.java`
    - `src/main/java/com/simplerender/gl/IndexBuffer.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/gl/VertexArray.java`
    - `src/main/java/com/simplerender/gl/VertexBuffer.java`
    - `src/main/java/com/simplerender/math/Vector3f.java`
    - `src/main/java/com/simplerender/render/buffer/RenderBuffer.java`
    - `src/main/java/com/simplerender/render/culling/FrustumCuller.java`
    - `src/main/java/com/simplerender/render/mesh/ChunkMesher.java`
    - `src/main/java/com/simplerender/render/mesh/MeshCache.java`
    - `src/main/java/com/simplerender/render/mesh/MeshData.java`
    - `src/main/java/com/simplerender/render/pipeline/RenderPipeline.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/java/com/simplerender/scene/SceneSnapshot.java`
    - `src/main/java/com/simplerender/world/ChunkBlockView.java`
    - `src/main/java/com/simplerender/world/ChunkMeshData.java`
    - `src/main/java/com/simplerender/world/ChunkMeshDataFactory.java`

- `280dd0d` demo 1
  - Files:
    - `.idea/vcs.xml`

- `1bdfd44` Allow continuous game loop
  - Files:
    - `src/main/java/com/simplerender/app/GameLoop.java`

- `580f698` loop update
  - Files: (không có thay đổi trực tiếp trong commit này)

- `7a8c6b6` Run continuously by default
  - Files:
    - `src/main/java/com/simplerender/app/EngineConfig.java`

- `786f43b` Add lit cubes and camera view
  - Files:
    - `src/main/java/com/simplerender/camera/Camera.java`
    - `src/main/java/com/simplerender/camera/CameraController.java`
    - `src/main/java/com/simplerender/camera/CameraSnapshot.java`
    - `src/main/java/com/simplerender/gl/GPUMesh.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/RenderUniforms.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/math/Matrix4f.java`
    - `src/main/java/com/simplerender/math/Vector3f.java`
    - `src/main/java/com/simplerender/world/ChunkMeshDataFactory.java`

- `7a921e0` Add creative camera input controls
  - Files:
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/InputState.java`
    - `src/main/java/com/simplerender/camera/CameraController.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/scene/Scene.java`

- `77de913` Add handle-based GPU resource pipeline
  - Files:
    - `build.gradle`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/asset/MaterialData.java`
    - `src/main/java/com/simplerender/asset/MeshData.java`
    - `src/main/java/com/simplerender/asset/MeshDataFactory.java`
    - `src/main/java/com/simplerender/asset/plugin/ModelImporter.java`
    - `src/main/java/com/simplerender/gl/GPUMesh.java`
    - `src/main/java/com/simplerender/gl/GpuResourceManager.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/render/MaterialHandle.java`
    - `src/main/java/com/simplerender/render/MeshHandle.java`
    - `src/main/java/com/simplerender/render/MeshUploader.java`
    - `src/main/java/com/simplerender/render/RenderItem.java`
    - `src/main/java/com/simplerender/render/pipeline/RenderPipeline.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/java/com/simplerender/scene/SceneSnapshot.java`

- `d87f4ab` Add texture support to materials
  - Files:
    - `src/main/java/com/simplerender/asset/MaterialData.java`
    - `src/main/java/com/simplerender/asset/TextureData.java`
    - `src/main/java/com/simplerender/asset/TextureDataFactory.java`
    - `src/main/java/com/simplerender/gl/GpuResourceManager.java`
    - `src/main/java/com/simplerender/gl/GpuTexture.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/render/MeshUploader.java`
    - `src/main/java/com/simplerender/render/TextureHandle.java`
    - `src/main/java/com/simplerender/scene/Scene.java`

- `363ad2e` Add PF4J OBJ/GLTF importers and model picker
  - Files:
    - `plugins/gltf-plugin/build.gradle`
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfImporterPlugin.java`
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `plugins/gltf-plugin/src/main/resources/plugin.properties`
    - `plugins/obj-plugin/build.gradle`
    - `plugins/obj-plugin/src/main/java/com/simplerender/plugin/obj/ObjImporterPlugin.java`
    - `plugins/obj-plugin/src/main/java/com/simplerender/plugin/obj/ObjModelImporter.java`
    - `plugins/obj-plugin/src/main/resources/plugin.properties`
    - `settings.gradle`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/ModelFileDialog.java`
    - `src/main/java/com/simplerender/app/ModelImportService.java`
    - `src/main/java/com/simplerender/scene/Scene.java`

- `38b93fd` Fix default texture selection
  - Files:
    - `src/main/java/com/simplerender/gl/GpuResourceManager.java`

- `2b0277a` Use PF4J development plugin manager
  - Files:
    - `src/main/java/com/simplerender/app/ModelImportService.java`

- `96e6d8b` Use custom PF4J dev plugin manager
  - Files:
    - `src/main/java/com/simplerender/app/ModelImportService.java`
    - `src/main/java/com/simplerender/app/SimpleRenderPluginManager.java`

- `8bd8568` Fix plugin discovery and add glb support
  - Files:
    - `build.gradle`
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `plugins/obj-plugin/src/main/java/com/simplerender/plugin/obj/ObjModelImporter.java`
    - `src/main/java/com/simplerender/app/ModelFileDialog.java`
    - `src/main/java/com/simplerender/app/ModelImportService.java`
    - `src/main/java/com/simplerender/asset/plugin/ModelImporter.java`

- `40b6b29` Harden plugin loading and jar copying
  - Files:
    - `build.gradle`
    - `src/main/java/com/simplerender/app/ModelImportService.java`

- `d46f5b1` Remove invalid PF4J classpath load
  - Files:
    - `src/main/java/com/simplerender/app/ModelImportService.java`

- `75a08be` Fix dev plugin jar discovery
  - Files:
    - `src/main/java/com/simplerender/app/ModelImportService.java`

- `1bd1f15` Prepare dev plugin descriptors
  - Files:
    - `src/main/java/com/simplerender/app/ModelImportService.java`

- `6497400` Build plugin classes for dev run
  - Files:
    - `build.gradle`

- `3fb0b5a` commit
  - Files:
    - `.idea/gradle.xml`
    - `.idea/vcs.xml`
    - `plugins/gltf-plugin/plugin.properties`
    - `plugins/obj-plugin/plugin.properties`
    - `src/main/java/com/simplerender/app/SimpleRenderPluginManager.java`

- `333b578` Use GUI model picker on startup
  - Files:
    - `src/main/java/com/simplerender/Main.java`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/model/ModelImportService.java`
    - `src/main/java/com/simplerender/model/ObjModelLoader.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/java/com/simplerender/ui/ModelSelectionDialog.java`

- `34cff30` Use model import plugins for selected models
  - Files:
    - `src/main/java/com/simplerender/Main.java`
    - `src/main/java/com/simplerender/model/ModelImportService.java`
    - `src/main/java/com/simplerender/model/ModelImporter.java`
    - `src/main/java/com/simplerender/model/ObjModelLoader.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/java/com/simplerender/ui/ModelSelectionDialog.java`

- `f83acec` merge demo
  - Files: (không có thay đổi trực tiếp trong commit này)

- `daea570` Bridge scene loading to existing model import service
  - Files:
    - `src/main/java/com/simplerender/model/ModelImportService.java`
    - `src/main/java/com/simplerender/model/ModelImporter.java`
    - `src/main/java/com/simplerender/scene/Scene.java`

- `799d756` Use existing model import service in Scene
  - Files:
    - `src/main/java/com/simplerender/scene/Scene.java`

- `bd46678` Merge branch 'master' into codex/investigate-model-loading-failure
  - Files: (không có thay đổi trực tiếp trong commit này)

- `a745678` Merge pull request #4 from d2mteam/codex/investigate-model-loading-failure
  - Files: (không có thay đổi trực tiếp trong commit này)

- `3f89f3a` Fix scene bootstrap and input handling
  - Files:
    - `src/main/java/com/simplerender/scene/Scene.java`

- `5ac7ba3` Merge pull request #5 from d2mteam/codex/fix-compilation-errors-in-scene.java
  - Files: (không có thay đổi trực tiếp trong commit này)

- `474d645` Add idle input state and scene update overload
  - Files:
    - `gradlew`
    - `src/main/java/com/simplerender/app/InputState.java`
    - `src/main/java/com/simplerender/scene/Scene.java`

- `eae691e` Merge pull request #6 from d2mteam/codex/fix-compilation-errors-in-java-project
  - Files: (không có thay đổi trực tiếp trong commit này)

- `7e57d70` Default to continuous game loop
  - Files:
    - `gradlew`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/InputState.java`
    - `src/main/java/com/simplerender/scene/Scene.java`

- `57509a7` Merge pull request #7 from d2mteam/codex/fix-compilation-errors-in-java-project-rcw7lb
  - Files: (không có thay đổi trực tiếp trong commit này)

- `390f902` Fail when selected model import fails
  - Files:
    - `gradlew`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/InputState.java`
    - `src/main/java/com/simplerender/scene/Scene.java`

- `1394c79` Merge pull request #9 from d2mteam/codex/fix-compilation-errors-in-java-project-088hut
  - Files: (không có thay đổi trực tiếp trong commit này)

- `f6e744a` new
  - Files:
    - `src/main/java/com/simplerender/app/GameLoop.java`

- `2d12aa3` Add detailed model import logging
  - Files:
    - `gradlew`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/InputState.java`
    - `src/main/java/com/simplerender/app/ModelFileDialog.java`
    - `src/main/java/com/simplerender/app/ModelImportService.java`
    - `src/main/java/com/simplerender/scene/Scene.java`

- `0504731` Merge branch 'master' into codex/fix-compilation-errors-in-java-project-b54fkq
  - Files: (không có thay đổi trực tiếp trong commit này)

- `899e099` Merge pull request #10 from d2mteam/codex/fix-compilation-errors-in-java-project-b54fkq
  - Files: (không có thay đổi trực tiếp trong commit này)

- `e372dbd` Merge remote-tracking branch 'origin/master'
  - Files: (không có thay đổi trực tiếp trong commit này)

- `25d11e1` Fix plugin importer discovery and logging
  - Files:
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/ModelFileDialog.java`
    - `src/main/java/com/simplerender/app/ModelImportService.java`
    - `src/main/java/com/simplerender/scene/Scene.java`

- `f1da539` Add gson dependency for plugin importers
  - Files:
    - `build.gradle`

- `5da01f3` Merge branch 'master' into codex/fix-compilation-errors-in-java-project-89sm1l
  - Files: (không có thay đổi trực tiếp trong commit này)

- `9450ac8` Merge pull request #11 from d2mteam/codex/fix-compilation-errors-in-java-project-89sm1l
  - Files: (không có thay đổi trực tiếp trong commit này)

- `f2548d2` Ensure PF4J extension index and Gson availability
  - Files:
    - `build.gradle`
    - `plugins/gltf-plugin/build.gradle`
    - `plugins/gltf-plugin/src/main/resources/META-INF/extensions.idx`
    - `plugins/obj-plugin/build.gradle`
    - `plugins/obj-plugin/src/main/resources/META-INF/extensions.idx`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/ModelFileDialog.java`
    - `src/main/java/com/simplerender/app/ModelImportService.java`
    - `src/main/java/com/simplerender/scene/Scene.java`

- `1b4ad27` Merge branch 'master' into codex/fix-compilation-errors-in-java-project-dq8mt8
  - Files: (không có thay đổi trực tiếp trong commit này)

- `916b49b` Merge pull request #12 from d2mteam/codex/fix-compilation-errors-in-java-project-dq8mt8
  - Files: (không có thay đổi trực tiếp trong commit này)

- `7d04c62` Add texture support and shader resources
  - Files:
    - `build.gradle`
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `plugins/gltf-plugin/src/main/resources/META-INF/extensions.idx`
    - `plugins/obj-plugin/src/main/resources/META-INF/extensions.idx`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/ModelFileDialog.java`
    - `src/main/java/com/simplerender/app/ModelImportService.java`
    - `src/main/java/com/simplerender/asset/ShaderSource.java`
    - `src/main/java/com/simplerender/asset/ShaderSourceLoader.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/default.vert`

- `6c0ba34` Add Disney BRDF shader option
  - Files:
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/asset/ShaderSourceLoader.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/resources/shaders/disney_brdf.frag`
    - `src/main/resources/shaders/disney_brdf.vert`

- `3ca945d` Merge branch 'master' into codex/fix-compilation-errors-in-java-project-heiyzj
  - Files: (không có thay đổi trực tiếp trong commit này)

- `bb18400` Merge pull request #13 from d2mteam/codex/fix-compilation-errors-in-java-project-heiyzj
  - Files: (không có thay đổi trực tiếp trong commit này)

- `442d95b` Add glTF material textures
  - Files:
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `src/main/java/com/simplerender/asset/MaterialData.java`

- `f723eaa` Merge branch 'master' into codex/update-gltfmodelimporter-for-texture-support
  - Files: (không có thay đổi trực tiếp trong commit này)

- `2213dff` Merge pull request #14 from d2mteam/codex/update-gltfmodelimporter-for-texture-support
  - Files: (không có thay đổi trực tiếp trong commit này)

- `1ac1ac8` Support multiple material textures
  - Files:
    - `src/main/java/com/simplerender/asset/MaterialData.java`
    - `src/main/java/com/simplerender/gl/GpuResourceManager.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/disney_brdf.frag`

- `0e73a95` Merge branch 'master' into codex/update-materialdata-to-support-multiple-textures
  - Files: (không có thay đổi trực tiếp trong commit này)

- `d2e3021` Merge pull request #15 from d2mteam/codex/update-materialdata-to-support-multiple-textures
  - Files: (không có thay đổi trực tiếp trong commit này)

- `0555fb1` Update shaders for PBR textures
  - Files:
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/disney_brdf.frag`

- `3765aa4` Merge pull request #16 from d2mteam/codex/update-fragment-shader-for-new-textures
  - Files: (không có thay đổi trực tiếp trong commit này)

- `582519c` Support named shader loading
  - Files:
    - `src/main/java/com/simplerender/Main.java`
    - `src/main/java/com/simplerender/asset/ShaderSourceLoader.java`

- `a52693e` Merge pull request #17 from d2mteam/codex/extend-shadersourceloader-for-multiple-shaders
  - Files: (không có thay đổi trực tiếp trong commit này)

- `27a092c` Add JavaFX render control panel
  - Files:
    - `build.gradle`
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `plugins/gltf-plugin/src/main/resources/META-INF/extensions.idx`
    - `plugins/obj-plugin/src/main/resources/META-INF/extensions.idx`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/ModelFileDialog.java`
    - `src/main/java/com/simplerender/app/ModelImportService.java`
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/asset/ShaderSource.java`
    - `src/main/java/com/simplerender/asset/ShaderSourceLoader.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/render/RenderItem.java`
    - `src/main/java/com/simplerender/render/Transform.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/default.vert`
    - `src/main/resources/shaders/disney_brdf.frag`
    - `src/main/resources/shaders/disney_brdf.vert`

- `f31b70f` Merge branch 'master' into codex/fix-compilation-errors-in-java-project-w8teao
  - Files: (không có thay đổi trực tiếp trong commit này)

- `50053a5` Merge pull request #18 from d2mteam/codex/fix-compilation-errors-in-java-project-w8teao
  - Files: (không có thay đổi trực tiếp trong commit này)

- `731f384` Add texture binding helper in renderer
  - Files:
    - `build.gradle`
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `plugins/gltf-plugin/src/main/resources/META-INF/extensions.idx`
    - `plugins/obj-plugin/src/main/resources/META-INF/extensions.idx`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/ModelFileDialog.java`
    - `src/main/java/com/simplerender/app/ModelImportService.java`
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/asset/ShaderSource.java`
    - `src/main/java/com/simplerender/asset/ShaderSourceLoader.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/render/RenderItem.java`
    - `src/main/java/com/simplerender/render/Transform.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/default.vert`
    - `src/main/resources/shaders/disney_brdf.frag`
    - `src/main/resources/shaders/disney_brdf.vert`

- `f78f345` Merge branch 'master' into codex/fix-compilation-errors-in-java-project-54hiwh
  - Files: (không có thay đổi trực tiếp trong commit này)

- `5c20eea` Merge pull request #19 from d2mteam/codex/fix-compilation-errors-in-java-project-54hiwh
  - Files: (không có thay đổi trực tiếp trong commit này)

- `a8f6c09` Fix renderer construction and texture handle access
  - Files:
    - `build.gradle`
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `plugins/gltf-plugin/src/main/resources/META-INF/extensions.idx`
    - `plugins/obj-plugin/src/main/resources/META-INF/extensions.idx`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/ModelFileDialog.java`
    - `src/main/java/com/simplerender/app/ModelImportService.java`
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/asset/ShaderSource.java`
    - `src/main/java/com/simplerender/asset/ShaderSourceLoader.java`
    - `src/main/java/com/simplerender/gl/GpuResourceManager.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/render/RenderItem.java`
    - `src/main/java/com/simplerender/render/Transform.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/default.vert`
    - `src/main/resources/shaders/disney_brdf.frag`
    - `src/main/resources/shaders/disney_brdf.vert`

- `81778cb` Merge branch 'master' into codex/fix-compilation-errors-in-java-project-i6x2cl
  - Files: (không có thay đổi trực tiếp trong commit này)

- `57715d2` Merge pull request #20 from d2mteam/codex/fix-compilation-errors-in-java-project-i6x2cl
  - Files: (không có thay đổi trực tiếp trong commit này)

- `c578171` Fix renderer lifecycle and input separation
  - Files:
    - `build.gradle`
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `plugins/gltf-plugin/src/main/resources/META-INF/extensions.idx`
    - `plugins/obj-plugin/src/main/resources/META-INF/extensions.idx`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/ModelFileDialog.java`
    - `src/main/java/com/simplerender/app/ModelImportService.java`
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/asset/ShaderSource.java`
    - `src/main/java/com/simplerender/asset/ShaderSourceLoader.java`
    - `src/main/java/com/simplerender/gl/GlfwInputReader.java`
    - `src/main/java/com/simplerender/gl/GpuResourceManager.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/render/RenderItem.java`
    - `src/main/java/com/simplerender/render/Transform.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/default.vert`
    - `src/main/resources/shaders/disney_brdf.frag`
    - `src/main/resources/shaders/disney_brdf.vert`

- `b809e34` Merge branch 'master' into codex/fix-compilation-errors-in-java-project-xpp7is
  - Files: (không có thay đổi trực tiếp trong commit này)

- `0ae1684` Merge pull request #21 from d2mteam/codex/fix-compilation-errors-in-java-project-xpp7is
  - Files: (không có thay đổi trực tiếp trong commit này)

- `84177e6` Log shader swaps
  - Files:
    - `build.gradle`
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `plugins/gltf-plugin/src/main/resources/META-INF/extensions.idx`
    - `plugins/obj-plugin/src/main/resources/META-INF/extensions.idx`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/ModelFileDialog.java`
    - `src/main/java/com/simplerender/app/ModelImportService.java`
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/asset/ShaderSource.java`
    - `src/main/java/com/simplerender/asset/ShaderSourceLoader.java`
    - `src/main/java/com/simplerender/gl/GlfwInputReader.java`
    - `src/main/java/com/simplerender/gl/GpuResourceManager.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/render/RenderItem.java`
    - `src/main/java/com/simplerender/render/Transform.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/default.vert`
    - `src/main/resources/shaders/disney_brdf.frag`
    - `src/main/resources/shaders/disney_brdf.vert`

- `9d57988` Merge branch 'master' into codex/fix-compilation-errors-in-java-project-8ielvl
  - Files: (không có thay đổi trực tiếp trong commit này)

- `e79f365` Merge pull request #22 from d2mteam/codex/fix-compilation-errors-in-java-project-8ielvl
  - Files: (không có thay đổi trực tiếp trong commit này)

- `ec0e52f` Add multi-texture material support
  - Files:
    - `build.gradle`
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `plugins/gltf-plugin/src/main/resources/META-INF/extensions.idx`
    - `plugins/obj-plugin/src/main/resources/META-INF/extensions.idx`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/ModelFileDialog.java`
    - `src/main/java/com/simplerender/app/ModelImportService.java`
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/asset/MaterialData.java`
    - `src/main/java/com/simplerender/asset/ShaderSource.java`
    - `src/main/java/com/simplerender/asset/ShaderSourceLoader.java`
    - `src/main/java/com/simplerender/gl/GlfwInputReader.java`
    - `src/main/java/com/simplerender/gl/GpuResourceManager.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/render/RenderItem.java`
    - `src/main/java/com/simplerender/render/Transform.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/default.vert`
    - `src/main/resources/shaders/disney_brdf.frag`
    - `src/main/resources/shaders/disney_brdf.vert`

- `e8a1756` Merge branch 'master' into codex/fix-compilation-errors-in-java-project-24md30
  - Files: (không có thay đổi trực tiếp trong commit này)

- `ba03d40` Merge pull request #23 from d2mteam/codex/fix-compilation-errors-in-java-project-24md30
  - Files: (không có thay đổi trực tiếp trong commit này)

- `6a6e2fc` Anchor sidebar to render window
  - Files:
    - `build.gradle`
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `plugins/gltf-plugin/src/main/resources/META-INF/extensions.idx`
    - `plugins/obj-plugin/src/main/resources/META-INF/extensions.idx`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/ModelFileDialog.java`
    - `src/main/java/com/simplerender/app/ModelImportService.java`
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/asset/MaterialData.java`
    - `src/main/java/com/simplerender/asset/ShaderSource.java`
    - `src/main/java/com/simplerender/asset/ShaderSourceLoader.java`
    - `src/main/java/com/simplerender/gl/GlfwInputReader.java`
    - `src/main/java/com/simplerender/gl/GpuResourceManager.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/render/RenderItem.java`
    - `src/main/java/com/simplerender/render/Transform.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/default.vert`
    - `src/main/resources/shaders/disney_brdf.frag`
    - `src/main/resources/shaders/disney_brdf.vert`

- `75b110d` Merge branch 'master' into codex/fix-compilation-errors-in-java-project-dxzl7n
  - Files: (không có thay đổi trực tiếp trong commit này)

- `b6a2e56` Merge pull request #24 from d2mteam/codex/fix-compilation-errors-in-java-project-dxzl7n
  - Files: (không có thay đổi trực tiếp trong commit này)

- `4bf1055` Queue renderer uploads on render thread
  - Files:
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`

- `b11cb64` Merge pull request #25 from d2mteam/codex/fix-javafx-context-initialization-error
  - Files: (không có thay đổi trực tiếp trong commit này)

- `24e83b5` Colorize logs and reduce render loop noise
  - Files:
    - `build.gradle`
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/resources/logback.xml`

- `7e1e9c8` Merge pull request #26 from d2mteam/codex/fix-javafx-context-initialization-error-115vvy
  - Files: (không có thay đổi trực tiếp trong commit này)

- `c14e8c1` Separate imported models from chunk rendering
  - Files:
    - `build.gradle`
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/resources/logback.xml`

- `23ac636` Merge branch 'master' into codex/fix-javafx-context-initialization-error-c8l81w
  - Files: (không có thay đổi trực tiếp trong commit này)

- `bd20aba` Merge pull request #27 from d2mteam/codex/fix-javafx-context-initialization-error-c8l81w
  - Files: (không có thay đổi trực tiếp trong commit này)

- `d548ebc` Dock render window alongside controls
  - Files:
    - `build.gradle`
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/RenderUniforms.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/resources/logback.xml`

- `770c86c` Merge pull request #28 from d2mteam/codex/fix-javafx-context-initialization-error-l85mka
  - Files: (không có thay đổi trực tiếp trong commit này)

- `577bb3f` Run window resize on render thread
  - Files:
    - `build.gradle`
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/RenderUniforms.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/resources/logback.xml`

- `be69cb2` Merge branch 'master' into codex/fix-javafx-context-initialization-error-lvp3e5
  - Files: (không có thay đổi trực tiếp trong commit này)

- `14b5df1` Merge pull request #29 from d2mteam/codex/fix-javafx-context-initialization-error-lvp3e5
  - Files: (không có thay đổi trực tiếp trong commit này)

- `76be84d` Fix WASD strafe and stop look outside render
  - Files:
    - `build.gradle`
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/camera/CameraController.java`
    - `src/main/java/com/simplerender/gl/GlfwInputReader.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/RenderUniforms.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/resources/logback.xml`

- `6536333` Merge branch 'master' into codex/fix-javafx-context-initialization-error-gz8poe
  - Files: (không có thay đổi trực tiếp trong commit này)

- `b0659db` Merge pull request #30 from d2mteam/codex/fix-javafx-context-initialization-error-gz8poe
  - Files: (không có thay đổi trực tiếp trong commit này)

- `f2c5ba0` Stabilize render window sync and cursor look
  - Files:
    - `build.gradle`
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/camera/CameraController.java`
    - `src/main/java/com/simplerender/gl/GlfwInputReader.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/RenderUniforms.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/resources/logback.xml`

- `a8374e5` Merge branch 'master' into codex/fix-javafx-context-initialization-error-9r387e
  - Files: (không có thay đổi trực tiếp trong commit này)

- `2bdec48` Merge pull request #31 from d2mteam/codex/fix-javafx-context-initialization-error-9r387e
  - Files: (không có thay đổi trực tiếp trong commit này)

- `77efc14` Add JavaFX render surface bridge
  - Files:
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/JavaFxInputAdapter.java`
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/app/RenderFrameBridge.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`

- `d4a15b7` Merge pull request #33 from d2mteam/codex/implement-render-surface-in-javafx
  - Files: (không có thay đổi trực tiếp trong commit này)

- `1c734b8` Fix JavaFX pixel format usage
  - Files:
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/app/GameLoop.java`
    - `src/main/java/com/simplerender/app/JavaFxInputAdapter.java`
    - `src/main/java/com/simplerender/app/RenderControlPanel.java`
    - `src/main/java/com/simplerender/app/RenderFrameBridge.java`
    - `src/main/java/com/simplerender/gl/GlfwInputReader.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`

- `1f2683a` Merge branch 'master' into codex/implement-render-surface-in-javafx-g8wxn1
  - Files: (không có thay đổi trực tiếp trong commit này)

- `246bdda` Merge pull request #34 from d2mteam/codex/implement-render-surface-in-javafx-g8wxn1
  - Files: (không có thay đổi trực tiếp trong commit này)

- `aea16da` Fix frustum culling for arbitrary meshes
  - Files:
    - `src/main/java/com/simplerender/asset/MeshData.java`
    - `src/main/java/com/simplerender/gl/GPUMesh.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/math/Matrix4f.java`
    - `src/main/java/com/simplerender/render/Transform.java`
    - `src/main/java/com/simplerender/render/culling/FrustumCuller.java`
    - `src/main/java/com/simplerender/render/pipeline/RenderPipeline.java`

- `4f792ed` Merge pull request #35 from d2mteam/codex/fix-frustum-culling-for-non-cube-models
  - Files: (không có thay đổi trực tiếp trong commit này)

- `afe83d3` Disable frustum culling for rendering
  - Files:
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `src/main/java/com/simplerender/asset/MeshData.java`
    - `src/main/java/com/simplerender/gl/GPUMesh.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/math/Matrix4f.java`
    - `src/main/java/com/simplerender/render/Transform.java`
    - `src/main/java/com/simplerender/render/culling/FrustumCuller.java`
    - `src/main/java/com/simplerender/render/pipeline/RenderPipeline.java`

- `bf3bbbb` Merge branch 'master' into codex/fix-frustum-culling-for-non-cube-models-3tpeoc
  - Files: (không có thay đổi trực tiếp trong commit này)

- `42dc38b` Merge pull request #36 from d2mteam/codex/fix-frustum-culling-for-non-cube-models-3tpeoc
  - Files: (không có thay đổi trực tiếp trong commit này)

- `cdffa2d` Marshal GL uploads and sampler bindings
  - Files:
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`

- `6003545` Merge pull request #37 from d2mteam/codex/fix-rendering-issues-and-data-races
  - Files: (không có thay đổi trực tiếp trong commit này)

- `91be10f` Remove chunk-based scene setup
  - Files:
    - `src/main/java/com/simplerender/Main.java`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/asset/MeshDataFactory.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/render/mesh/ChunkMesher.java`
    - `src/main/java/com/simplerender/render/mesh/MeshCache.java`
    - `src/main/java/com/simplerender/render/mesh/MeshData.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/java/com/simplerender/world/ChunkBlockView.java`
    - `src/main/java/com/simplerender/world/ChunkMeshData.java`
    - `src/main/java/com/simplerender/world/ChunkMeshDataFactory.java`

- `a393703` Add mesh UVs for textured rendering
  - Files:
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `plugins/obj-plugin/src/main/java/com/simplerender/plugin/obj/ObjModelImporter.java`
    - `src/main/java/com/simplerender/asset/MeshData.java`
    - `src/main/java/com/simplerender/gl/GPUMesh.java`
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/default.vert`
    - `src/main/resources/shaders/disney_brdf.frag`
    - `src/main/resources/shaders/disney_brdf.vert`

- `12da963` Merge pull request #38 from d2mteam/codex/fix-rendering-issues-and-data-races-ctqvxz
  - Files: (không có thay đổi trực tiếp trong commit này)

- `15b5051` Support normal and metallic texture maps
  - Files:
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `plugins/obj-plugin/src/main/java/com/simplerender/plugin/obj/ObjModelImporter.java`
    - `src/main/java/com/simplerender/Main.java`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/asset/MeshData.java`
    - `src/main/java/com/simplerender/asset/MeshDataFactory.java`
    - `src/main/java/com/simplerender/asset/TextureDataFactory.java`
    - `src/main/java/com/simplerender/gl/GPUMesh.java`
    - `src/main/java/com/simplerender/gl/GpuResourceManager.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/render/mesh/ChunkMesher.java`
    - `src/main/java/com/simplerender/render/mesh/MeshCache.java`
    - `src/main/java/com/simplerender/render/mesh/MeshData.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/java/com/simplerender/world/ChunkBlockView.java`
    - `src/main/java/com/simplerender/world/ChunkMeshData.java`
    - `src/main/java/com/simplerender/world/ChunkMeshDataFactory.java`
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/default.vert`
    - `src/main/resources/shaders/disney_brdf.frag`
    - `src/main/resources/shaders/disney_brdf.vert`

- `2b2f473` Merge branch 'master' into codex/fix-rendering-issues-and-data-races-dfvrhq
  - Files: (không có thay đổi trực tiếp trong commit này)

- `18f9060` Merge pull request #39 from d2mteam/codex/fix-rendering-issues-and-data-races-dfvrhq
  - Files: (không có thay đổi trực tiếp trong commit này)

- `ef80622` Add sampler resources and glTF sampler import
  - Files:
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `plugins/obj-plugin/src/main/java/com/simplerender/plugin/obj/ObjModelImporter.java`
    - `src/main/java/com/simplerender/Main.java`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/asset/MaterialData.java`
    - `src/main/java/com/simplerender/asset/MeshData.java`
    - `src/main/java/com/simplerender/asset/MeshDataFactory.java`
    - `src/main/java/com/simplerender/asset/SamplerData.java`
    - `src/main/java/com/simplerender/asset/TextureDataFactory.java`
    - `src/main/java/com/simplerender/asset/TextureSlot.java`
    - `src/main/java/com/simplerender/gl/GPUMesh.java`
    - `src/main/java/com/simplerender/gl/GpuResourceManager.java`
    - `src/main/java/com/simplerender/gl/GpuSampler.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/render/SamplerHandle.java`
    - `src/main/java/com/simplerender/render/mesh/ChunkMesher.java`
    - `src/main/java/com/simplerender/render/mesh/MeshCache.java`
    - `src/main/java/com/simplerender/render/mesh/MeshData.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/java/com/simplerender/world/ChunkBlockView.java`
    - `src/main/java/com/simplerender/world/ChunkMeshData.java`
    - `src/main/java/com/simplerender/world/ChunkMeshDataFactory.java`
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/default.vert`
    - `src/main/resources/shaders/disney_brdf.frag`
    - `src/main/resources/shaders/disney_brdf.vert`

- `720e255` Merge branch 'master' into codex/fix-rendering-issues-and-data-races-9guvp3
  - Files: (không có thay đổi trực tiếp trong commit này)

- `3f81e97` Merge pull request #40 from d2mteam/codex/fix-rendering-issues-and-data-races-9guvp3
  - Files: (không có thay đổi trực tiếp trong commit này)

- `2e33c97` Improve texture upload filtering and alignment
  - Files:
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `plugins/obj-plugin/src/main/java/com/simplerender/plugin/obj/ObjModelImporter.java`
    - `src/main/java/com/simplerender/Main.java`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/asset/MaterialData.java`
    - `src/main/java/com/simplerender/asset/MeshData.java`
    - `src/main/java/com/simplerender/asset/MeshDataFactory.java`
    - `src/main/java/com/simplerender/asset/SamplerData.java`
    - `src/main/java/com/simplerender/asset/TextureDataFactory.java`
    - `src/main/java/com/simplerender/asset/TextureSlot.java`
    - `src/main/java/com/simplerender/gl/GPUMesh.java`
    - `src/main/java/com/simplerender/gl/GpuResourceManager.java`
    - `src/main/java/com/simplerender/gl/GpuSampler.java`
    - `src/main/java/com/simplerender/gl/GpuTexture.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/render/SamplerHandle.java`
    - `src/main/java/com/simplerender/render/mesh/ChunkMesher.java`
    - `src/main/java/com/simplerender/render/mesh/MeshCache.java`
    - `src/main/java/com/simplerender/render/mesh/MeshData.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/java/com/simplerender/world/ChunkBlockView.java`
    - `src/main/java/com/simplerender/world/ChunkMeshData.java`
    - `src/main/java/com/simplerender/world/ChunkMeshDataFactory.java`
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/default.vert`
    - `src/main/resources/shaders/disney_brdf.frag`
    - `src/main/resources/shaders/disney_brdf.vert`

- `45c1fb1` Add tangent basis support for normal mapping
  - Files:
    - `src/main/java/com/simplerender/asset/MeshData.java`
    - `src/main/java/com/simplerender/gl/GPUMesh.java`
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/default.vert`
    - `src/main/resources/shaders/disney_brdf.frag`
    - `src/main/resources/shaders/disney_brdf.vert`

- `4df0637` Merge branch 'master' into codex/fix-rendering-issues-and-data-races-h13kml
  - Files: (không có thay đổi trực tiếp trong commit này)

- `529d15e` Merge pull request #41 from d2mteam/codex/fix-rendering-issues-and-data-races-h13kml
  - Files: (không có thay đổi trực tiếp trong commit này)

- `63257aa` Use inverse-transpose normal matrix for TBN
  - Files:
    - `plugins/gltf-plugin/src/main/java/com/simplerender/plugin/gltf/GltfModelImporter.java`
    - `plugins/obj-plugin/src/main/java/com/simplerender/plugin/obj/ObjModelImporter.java`
    - `src/main/java/com/simplerender/Main.java`
    - `src/main/java/com/simplerender/app/EngineConfig.java`
    - `src/main/java/com/simplerender/app/GameApplication.java`
    - `src/main/java/com/simplerender/asset/MaterialData.java`
    - `src/main/java/com/simplerender/asset/MeshData.java`
    - `src/main/java/com/simplerender/asset/MeshDataFactory.java`
    - `src/main/java/com/simplerender/asset/SamplerData.java`
    - `src/main/java/com/simplerender/asset/TextureDataFactory.java`
    - `src/main/java/com/simplerender/asset/TextureSlot.java`
    - `src/main/java/com/simplerender/gl/GPUMesh.java`
    - `src/main/java/com/simplerender/gl/GpuResourceManager.java`
    - `src/main/java/com/simplerender/gl/GpuSampler.java`
    - `src/main/java/com/simplerender/gl/GpuTexture.java`
    - `src/main/java/com/simplerender/gl/OpenGLRenderer.java`
    - `src/main/java/com/simplerender/gl/ShaderProgram.java`
    - `src/main/java/com/simplerender/render/SamplerHandle.java`
    - `src/main/java/com/simplerender/render/mesh/ChunkMesher.java`
    - `src/main/java/com/simplerender/render/mesh/MeshCache.java`
    - `src/main/java/com/simplerender/render/mesh/MeshData.java`
    - `src/main/java/com/simplerender/scene/RenderableChunk.java`
    - `src/main/java/com/simplerender/scene/Scene.java`
    - `src/main/java/com/simplerender/world/ChunkBlockView.java`
    - `src/main/java/com/simplerender/world/ChunkMeshData.java`
    - `src/main/java/com/simplerender/world/ChunkMeshDataFactory.java`
    - `src/main/resources/shaders/default.frag`
    - `src/main/resources/shaders/default.vert`
    - `src/main/resources/shaders/disney_brdf.frag`
    - `src/main/resources/shaders/disney_brdf.vert`

- `0f89c00` Merge branch 'master' into codex/fix-rendering-issues-and-data-races-lgei8b
  - Files: (không có thay đổi trực tiếp trong commit này)

- `2e66889` Merge pull request #42 from d2mteam/codex/fix-rendering-issues-and-data-races-lgei8b
  - Files: (không có thay đổi trực tiếp trong commit này)
