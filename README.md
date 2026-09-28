# Simple Render (Java)

Engine render 3D tối giản viết bằng Java 21 + LWJGL (OpenGL 3.3), kèm giao diện desktop JavaFX.
Mục tiêu của project: **đơn giản, dễ đọc, kiến trúc rõ ràng**.

## Hai lõi tách biệt: `engine` và `ui`

```
┌──────────────────────── ui/  (JavaFX) ────────────────────────┐
│  SimpleRenderApp · ControlPanel · FrameView · InputCollector  │
└───────────────┬───────────────────────────────▲───────────────┘
   Engine API   │ requestShader / requestResize │ EngineHost
   (UI → engine)│ requestPostProcessSettings    │ (engine → UI)
                │ loadModel / scene()           │ pollInput / onFrame / engineStopped
┌───────────────▼───────────────────────────────┴───────────────┐
│  engine/  (không có JavaFX trên classpath)                    │
│    engine ──► render (+ render.gl) ──► scene ──► asset ──► math│
└───────────────────────────────▲───────────────────────────────┘
                                │ ModelImporter (PF4J)
                 plugins/obj-plugin · plugins/gltf-plugin
```

| Module | Vai trò |
| --- | --- |
| `engine/` | Lõi render. Không phụ thuộc JavaFX, dùng được từ bất kỳ front-end nào (UI, test, tool offline). |
| `ui/` | App desktop JavaFX. Chỉ dùng API công khai của engine. |
| `plugins/` | Importer OBJ và glTF (có hỗ trợ `.glb`, sparse accessor, Draco), được nạp lúc chạy bằng PF4J. |

Ranh giới giữa hai lõi được Gradle đảm bảo: module `engine` không thể import JavaFX.
Bên trong engine, `ArchitectureTest` kiểm tra rằng các tầng chỉ phụ thuộc xuống dưới.

## Chạy

```bash
gradle run                                           # mở app, rồi bấm "Load model…"
gradle run --args="--model test.obj"
gradle run --args="--model model.gltf --shader disney_brdf"
gradle test
```

| Tham số | Ý nghĩa |
| --- | --- |
| `--model <file>` | Model load lúc khởi động (`.obj`, `.gltf`, `.glb`) |
| `--shader <name>` | `default`, `disney_brdf` hoặc `debug_mesh` |
| `--max-frames <n>` | Tự thoát sau n frame (dùng cho smoke test) |

Điều khiển: click vào khung hình, dùng **W A S D** để di chuyển, **Space / Shift** để lên / xuống, **kéo chuột** để xoay camera.

Cần JDK 21. Gradle tự tìm JDK đã cài (JAVA_HOME, `/usr/lib/jvm`, `~/.jdks` của IntelliJ, SDKMAN, ...) qua Java toolchain,
nên không cần cấu hình đường dẫn.

## Cấu trúc code

```
engine/src/main/java/com/simplerender/
  math/          Vector3f, Matrix4f
  asset/         MeshData, MaterialData, TextureData, ...: dữ liệu CPU, bất biến
  asset/plugin/  ModelImporter (API cho plugin), ModelImportService (nạp plugin)
  scene/         Scene, SceneObject, Camera, CameraController, Light, Transform, SceneSnapshot
  render/        Renderer, ScenePass, PostProcessPass, GpuResourceCache, FrustumCuller, ShaderLibrary
  render/gl/     Wrapper OpenGL mỏng: GlContext, ShaderProgram, GpuMesh, GpuTexture, RenderTarget, ...
  engine/        Engine (render thread + vòng lặp), EngineHost, EngineConfig
engine/src/main/resources/shaders/   GLSL
ui/src/main/java/com/simplerender/ui/
  Main, SimpleRenderApp, ControlPanel, FrameView, InputCollector, LaunchOptions
```

Mỗi frame:

```
Scene ──snapshot()──► SceneSnapshot ──► ScenePass ──► PostProcessPass ──► đọc pixel ──► FrameView (JavaFX)
```

Xem chi tiết (luồng xử lý, threading, cách mở rộng) tại [docs/RENDER_ARCHITECTURE.md](docs/RENDER_ARCHITECTURE.md).
