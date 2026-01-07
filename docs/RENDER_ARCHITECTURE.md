# Kiến trúc render của Simple Render

## Tổng quan
Simple Render là một engine render chạy trên OpenGL với các lớp dữ liệu CPU/GPU tách biệt rõ ràng. Dòng chảy chính:

1. **GameApplication** khởi tạo renderer, plugin import và scene.
2. **Scene** cập nhật camera/transform và tạo **SceneSnapshot** để render thread tiêu thụ.
3. **OpenGLRenderer** nhận snapshot, cập nhật uniform, upload tài nguyên cần thiết và chạy **RenderGraph**.
4. **RenderGraph** thực thi các **RenderPass** theo thứ tự: scene pass → post process → (tuỳ chọn) ray tracing → readback.

## Dòng chảy dữ liệu render

```
Input (JavaFX) → InputState → CameraController → Scene
Scene → SceneSnapshot → OpenGLRenderer
OpenGLRenderer → RenderGraph → RenderPasses → GPU
```

### Scene và Camera
- **Scene** sở hữu camera và các **RenderItem** (mesh + material + transform).
- **CameraController** đọc **InputState** để cập nhật yaw/pitch và vị trí.
- **SceneSnapshot** đóng gói camera + danh sách render item để thread render dùng an toàn.

### Asset & import
- **ModelImportService** dùng PF4J để load plugin importer.
- **ModelImporter** xuất các **ImportedPrimitive** gồm `MeshData`, `MaterialData`, và transform.
- **TextureData**, **MaterialData**, **MeshData** đều là dữ liệu CPU-side, immutable để dễ chia sẻ.

### Quản lý tài nguyên GPU
- **GpuResourceManager** tạo handle và map handle → GPU resource.
- **MeshHandle / MaterialHandle / TextureHandle / SamplerHandle** là định danh ổn định.
- **GPUMesh** quản lý VAO/VBO/IBO cho một mesh.
- **GpuTexture** và **GpuSampler** ánh xạ texture/sampler CPU sang OpenGL.

### Upload pipeline
- **UploadQueue** gom các task upload để thực thi ở render thread.
- **StaticMeshUploader** và **DynamicVboUploader** hỗ trợ upload mesh/dữ liệu động.
- **TextureStreamingUploader** dùng PBO để streaming texture.
- **SparseTextureUploader** dùng sparse texture khi GPU hỗ trợ.

### RenderGraph và RenderPass
- **RenderGraph** là danh sách tuần tự các pass.
- **ScenePass**: vẽ geometry vào framebuffer chính.
- **PostProcessPass**: áp dụng tone mapping, bloom, color grading, v.v.
- **RayTracingPass**: chạy compute shader demo ray tracing.
- **ReadbackPass**: đọc lại texture để hiển thị trên JavaFX.

### Screen-space settings
- **ScreenSpaceSettings** chứa toàn bộ toggle/parameter hậu kỳ (bloom, DOF, SSR, v.v.).
- Các giá trị này được cập nhật từ **RenderControlPanel** và dùng làm uniform.

### Culling
- **RenderPipeline** và **FrustumCuller** thực hiện culling theo bounding sphere.
- **MeshData** cung cấp `boundsCenter()` và `boundsRadius()`.

## Sơ đồ lớp chính (tóm tắt)

- `com.simplerender.app`
  - **GameApplication**: khởi tạo app, importer, scene, UI.
  - **GameLoop**: vòng lặp update/render.
  - **RenderControlPanel**: UI điều khiển shader, hiệu ứng, transform.
  - **RenderFrameBridge**: copy buffer GPU → JavaFX ImageView.

- `com.simplerender.scene`
  - **Scene**: dữ liệu runtime của world.
  - **SceneSnapshot**: bản snapshot read-only cho render.

- `com.simplerender.render`
  - **RenderItem**: mesh + material + transform.
  - **Transform**: vị trí và scale.
  - **RenderPipeline**: culling.

- `com.simplerender.gl`
  - **OpenGLRenderer**: trung tâm render.
  - **GpuResourceManager**: quản lý GPU resources.
  - **RenderUniforms**: camera/lights/screen-space uniforms.
  - **ShaderProgram / ComputeShaderProgram**: shader wrappers.

- `com.simplerender.asset`
  - **MeshData / MaterialData / TextureData**: asset CPU-side.
  - **ShaderSourceLoader**: load shader từ resources.

- `com.simplerender.memory`
  - Các allocator mẫu (TLSF, Buddy, Pool, Slab, Stack, Arena) dùng cho thử nghiệm.

## Gợi ý mở rộng
- Thêm pass mới: tạo class implement **RenderPass**, add vào **RenderGraph**.
- Thêm hiệu ứng hậu kỳ: mở rộng **ScreenSpaceSettings** + shader post-process.
- Thêm import định dạng mới: implement **ModelImporter** và đóng gói plugin.
