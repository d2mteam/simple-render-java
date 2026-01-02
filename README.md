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

---

## Memory & Buffer Allocators (định hướng cho framework)
Phần này mô tả các cơ chế cấp bộ nhớ nên chuẩn bị cho framework (CPU-side buffers, staging, upload, scratch). Mục tiêu là **thay thế `malloc` mặc định** cho các tình huống cần hiệu năng, kiểm soát lifetime, hoặc giảm phân mảnh.

### 1) Linear / Arena Allocator
**Dùng khi:** dữ liệu có cùng lifetime theo frame hoặc theo phase (frame scratch, temporary).  
**Ưu điểm:** cực nhanh, reset O(1).  
**Nhược điểm:** không giải phóng riêng lẻ.  
**Khuyến nghị:** dùng cho **frame scratch**, CPU staging buffer, job system temporary memory.

### 2) Stack Allocator
**Dùng khi:** cấp phát theo LIFO (push/pop).  
**Ưu điểm:** nhanh, deterministic, dễ debug.  
**Nhược điểm:** phải giải phóng đúng thứ tự.  
**Khuyến nghị:** dùng cho **per-pass temporary**, shader compile staging, small transient buffers.

### 3) Pool Allocator
**Dùng khi:** nhiều đối tượng cùng kích thước (components, handles, small structs).  
**Ưu điểm:** constant-time alloc/free, tránh phân mảnh.  
**Nhược điểm:** chỉ phù hợp fixed-size.  
**Khuyến nghị:** dùng cho **RenderItem**, `MaterialHandle`, `MeshHandle`, hoặc objects có lifetime lâu.

### 4) Slab Allocator
**Dùng khi:** nhiều kích thước nhỏ khác nhau nhưng có nhóm rõ ràng.  
**Ưu điểm:** giảm phân mảnh cho small allocations, thường dùng trong kernel/low-level systems.  
**Nhược điểm:** quản lý phức tạp hơn Pool.  
**Khuyến nghị:** dùng cho **small resources** (descriptor-like data, small arrays, metadata).

### 5) Buddy Allocator
**Dùng khi:** cần cấp phát block lớn/nhỏ linh hoạt (GPU upload heap, texture atlas).  
**Ưu điểm:** split/merge theo power-of-two, dễ quản lý.  
**Nhược điểm:** internal fragmentation.  
**Khuyến nghị:** dùng cho **GPU heap emulation** hoặc large buffers với size biến thiên.

### 6) TLSF (Two-Level Segregated Fit)
**Dùng khi:** cần real-time allocation với O(1) và ít fragmentation.  
**Ưu điểm:** nhanh, deterministic, tốt cho runtime.  
**Nhược điểm:** cài đặt phức tạp hơn.  
**Khuyến nghị:** dùng cho **core runtime allocator** khi cần cấp phát/giải phóng thường xuyên.

### 7) malloc replacement (jemalloc, mimalloc, tcmalloc)
**Dùng khi:** muốn thay `malloc` toàn hệ thống bằng allocator tối ưu.  
**Ưu điểm:** cải thiện fragmentation, throughput.  
**Nhược điểm:** phụ thuộc runtime/platform.  
**Khuyến nghị:** dùng khi **scale lớn**, profiling cho thấy `malloc` là bottleneck.

---

## Gợi ý chọn allocator theo use-case
- **Frame temporary / scratch** → Linear / Arena.  
- **Per-pass temporary** → Stack.  
- **Handles/objects fixed-size** → Pool.  
- **Metadata nhỏ, nhiều kích cỡ** → Slab.  
- **Resource heap lớn** → Buddy.  
- **Runtime real-time alloc/free** → TLSF.  
- **Global replacement** → jemalloc / mimalloc / tcmalloc (tùy platform).
