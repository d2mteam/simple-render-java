# Simple Render Engine (Java)

## Checklist trước khi bắt đầu
- [ ] Xác định mục tiêu render chính (scene, lighting, post-processing) và shader mặc định cần dùng.
- [ ] Nắm rõ luồng dữ liệu: `Scene` -> `SceneSnapshot` -> `OpenGLRenderer`.
- [ ] Hiểu cấu trúc Render Graph: Scene Pass -> Post Process Pass -> Readback Pass.
- [ ] Kiểm tra tài nguyên GPU (mesh, texture, sampler) được upload qua `MeshUploader`.
- [ ] Xác nhận shader/texture path trong `src/main/resources/shaders/`.

---

## Tổng quan
Simple Render Engine là một engine render tối giản dựa trên LWJGL, được tổ chức theo phong cách mô-đun để dễ mở rộng. Tài liệu dưới đây tập trung vào các thành phần cốt lõi và giải thích vai trò từng class theo cách tương tự Unity: mục đích, trách nhiệm, và cách kết nối.

## Kiến trúc thư mục
```
src/main/java/com/simplerender/
  app/
  asset/
  camera/
  gl/
  gl/rendergraph/
  math/
  render/
  scene/
```

## Render Graph (Pipeline chính)
Render Graph là xương sống của pipeline, tổ chức các pass theo thứ tự rõ ràng:

1. **Scene Pass**: vẽ toàn bộ mesh vào framebuffer chính.
2. **Post Process Pass**: chạy screen-space post-processing trên texture màu/độ sâu.
3. **Readback Pass**: đọc kết quả cuối để đẩy lên UI (JavaFX bridge).

Các class liên quan:
- `com.simplerender.gl.rendergraph.RenderGraph`: danh sách pass và thứ tự thực thi.
- `com.simplerender.gl.rendergraph.RenderPass`: interface cho mỗi pass (`name()` và `execute()`).
- `com.simplerender.gl.rendergraph.RenderGraphContext`: ngữ cảnh chạy pass, chứa `OpenGLRenderer` và `SceneSnapshot`.

---

## Core Modules (tương tự Unity documentation)

### 1) Application Layer
**Mục tiêu**: điều phối vòng đời app, game loop, UI bridge.

- `com.simplerender.app.GameApplication`
  - Là entry-point của engine, khởi tạo và gắn renderer vào UI.
- `com.simplerender.app.GameLoop`
  - Điều phối `update()` và `render()` theo thời gian thực.
- `com.simplerender.app.RenderFrameBridge`
  - Nhận frame từ OpenGL và chuyển sang JavaFX.
- `com.simplerender.app.EngineConfig`
  - Cấu hình runtime: target FPS, chế độ loop, etc.

**Liên kết**: `GameLoop` gọi `OpenGLRenderer.render()` để chạy Render Graph.

---

### 2) Scene System
**Mục tiêu**: giữ trạng thái scene và tạo snapshot cho renderer.

- `com.simplerender.scene.Scene`
  - Lưu danh sách renderables, camera, dữ liệu tài nguyên.
- `com.simplerender.scene.SceneSnapshot`
  - Dữ liệu immutable để render (không phụ thuộc vào logic runtime).

**Luồng dữ liệu**:
`Scene` -> `SceneSnapshot` -> `OpenGLRenderer.render(snapshot)`

---

### 3) Camera
**Mục tiêu**: điều khiển góc nhìn, input camera, tạo snapshot.

- `com.simplerender.camera.Camera`
  - Vị trí, hướng nhìn, cấu hình view.
- `com.simplerender.camera.CameraController`
  - Điều khiển bằng input (WASD, mouse).
- `com.simplerender.camera.CameraSnapshot`
  - Snapshot cho renderer.

---

### 4) Render Core
**Mục tiêu**: cấu trúc render item, upload mesh/material, culling.

- `com.simplerender.render.RenderItem`
  - Định nghĩa mesh + material + transform cho pipeline.
- `com.simplerender.render.MeshUploader`
  - Interface upload GPU, được implement bởi `OpenGLRenderer`.
- `com.simplerender.render.pipeline.RenderPipeline`
  - Frustum culling và quyết định render.
- `com.simplerender.render.culling.FrustumCuller`
  - Tính toán culling theo camera.

---

### 5) OpenGL Layer
**Mục tiêu**: tất cả giao tiếp GPU và shader phải nằm ở đây.

- `com.simplerender.gl.OpenGLRenderer`
  - Trung tâm render: init GL, load shader, render graph, post-processing.
  - Quản lý framebuffer, texture, screen quad, upload light + uniforms.
- `com.simplerender.gl.ShaderProgram`
  - Compile/link shader, set uniform (vec2/3/4, mat4, float, int).
- `com.simplerender.gl.GpuResourceManager`
  - Cache GPU mesh/texture/sampler.
- `com.simplerender.gl.GPUMesh`
  - Mesh GPU (VAO/VBO/IBO).
- `com.simplerender.gl.GpuTexture`, `com.simplerender.gl.GpuSampler`
  - Quản lý texture và sampler trên GPU.

---

### 6) Lighting & Screen-Space
**Mục tiêu**: cung cấp hệ lighting đa nguồn sáng và pipeline post-processing.

- `com.simplerender.gl.RenderUniforms`
  - Chứa danh sách light (directional/point/spot) + `ScreenSpaceSettings`.
- `com.simplerender.gl.ScreenSpaceSettings`
  - Toggle/param cho tone mapping, bloom, color grading, DOF, motion blur,
    vignette, film grain, SSAO, SSR, SSGI, contact shadows.

Shader liên quan:
- `src/main/resources/shaders/default.vert`
- `src/main/resources/shaders/default.frag`
- `src/main/resources/shaders/disney_brdf.frag`
- `src/main/resources/shaders/screen_post.vert`
- `src/main/resources/shaders/screen_post.frag`

---

## Render Flow (tổng hợp)
```
Scene -> SceneSnapshot -> OpenGLRenderer.render()
  -> RenderGraph
     1) ScenePass (geometry + lighting)
     2) PostProcessPass (screen-space effects)
     3) ReadbackPass (gửi frame về UI)
```

---

## Quy tắc mở rộng
- Tất cả OpenGL calls đặt trong `com.simplerender.gl`.
- Thêm pass mới bằng cách implement `RenderPass` và add vào `RenderGraph`.
- Thêm shader mới trong `src/main/resources/shaders` và load qua `ShaderSourceLoader`.
- Render logic không được truy cập trực tiếp `Scene`; chỉ dùng `SceneSnapshot`.

---

## Gợi ý khi phát triển
- Khi thêm hiệu ứng mới: tạo pass riêng trong Render Graph (giữ đúng thứ tự).
- Khi thêm resource mới: upload qua `MeshUploader` để đảm bảo thread safety.
- Khi cần debug: bật log tại `OpenGLRenderer` và kiểm tra framebuffer status.
