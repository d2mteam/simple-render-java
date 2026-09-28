# Kiến trúc Simple Render

Tài liệu này giải thích cách engine được chia lớp, một frame chạy như thế nào, các thread nói chuyện với nhau
ra sao, và cách mở rộng.

## 1. Nguyên tắc thiết kế

1. **Hai lõi, một chiều phụ thuộc.** `ui` phụ thuộc `engine`, không bao giờ ngược lại. Engine không biết JavaFX tồn tại.
2. **Mỗi tầng chỉ phụ thuộc tầng dưới nó.** Chỉ tầng `render` được gọi OpenGL.
3. **Scene là dữ liệu CPU thuần.** Tạo object không cần OpenGL context; renderer tự upload lên GPU ở lần vẽ đầu tiên.
4. **Chỉ một class biết về thread.** `Engine` sở hữu render thread. `Renderer` và các pass là code đơn luồng, không có lock.
5. **Renderer chỉ đọc snapshot**, không bao giờ đọc scene đang được UI sửa.

## 2. Các tầng

| Tầng (package) | Trách nhiệm | Được phụ thuộc vào |
| --- | --- | --- |
| `math` | `Vector3f`, `Matrix4f` (column-major) | (không gì cả) |
| `asset` | Dữ liệu load từ file: `MeshData`, `MaterialData`, `TextureData`, `SamplerData`. Bất biến, an toàn khi chia sẻ giữa các thread. | `math` |
| `asset.plugin` | `ModelImporter` (extension point cho plugin), `ModelImportService` (nạp plugin PF4J, chọn importer theo đuôi file) | `asset` |
| `scene` | Thứ cần vẽ: `Scene`, `SceneObject`, `Transform`, `Camera`, `CameraController`, `Light`, `InputState`, `SceneSnapshot` | `asset`, `math` |
| `render` | Cách vẽ: `Renderer`, `ScenePass`, `PostProcessPass`, `GpuResourceCache`, `FrustumCuller`, `ShaderLibrary`, `PostProcessSettings` | `scene`, `asset`, `math`, `render.gl` |
| `render.gl` | Wrapper OpenGL mỏng, mỗi class quản lý một loại object GL: `GlContext`, `ShaderProgram`, `GpuMesh`, `GpuTexture`, `GpuSampler`, `RenderTarget`, `FullscreenQuad` | `asset` |
| `engine` | Facade `Engine`: render thread, vòng lặp, nhận yêu cầu từ UI; `EngineHost` là hợp đồng với front-end | tất cả ở trên |
| `ui` (module riêng) | JavaFX: cửa sổ, panel điều khiển, hiển thị frame, gom input | `engine` |

`ArchitectureTest` (trong `engine/src/test`) đọc các lệnh `import` và fail nếu một tầng import tầng phía trên nó.

## 3. Một frame chạy như thế nào

Vòng lặp nằm trong `Engine.runLoop()`, chạy trên thread `render-thread`:

```
1. applyRequests()        áp dụng yêu cầu mới nhất từ UI: resize, đổi shader, post-process settings
2. scene.update(dt, input) camera di chuyển theo InputState lấy từ host.pollInput()
3. scene.snapshot()        chép camera, đèn và (mesh, material, model matrix) của từng object
4. renderer.render(snapshot, host)
     ScenePass       ─► sceneTarget (màu RGBA8 + depth)
                        frustum culling → upload lazy qua GpuResourceCache → vẽ từng object
     PostProcessPass ─► finalTarget (màu)
                        một quad toàn màn hình chạy screen_post.frag (bloom, tone map, SSAO, ...)
     readBack        ─► host.onFrame(pixel BGRA, width, height)
5. ngủ cho tới frame kế tiếp (mặc định 60 FPS)
```

## 4. Mô hình thread

| Thread | Được làm | Không được làm |
| --- | --- | --- |
| JavaFX Application Thread | Dựng UI, xử lý sự kiện, gọi `engine.request...()`, `engine.scene()...` | Gọi OpenGL, chờ render thread |
| `render-thread` (của `Engine`) | Mọi lời gọi OpenGL (`Renderer`, các pass, `render.gl`) | Đụng vào control JavaFX |
| `model-import` (của `ControlPanel`) | `engine.loadModel(path)`: đọc file, dựng `MeshData`/`MaterialData`, thêm vào scene | Gọi OpenGL |

Cách dữ liệu đi qua ranh giới thread:

- **UI → engine:** các hàm `request...` chỉ ghi "mong muốn mới nhất" vào `AtomicReference`; render thread lấy ra ở đầu
  frame kế tiếp. Không cần hàng đợi task, không cần `CompletableFuture`.
- **Scene:** mọi method của `Scene` đều `synchronized`. UI thêm object hay sửa transform; render thread gọi
  `update()` và `snapshot()`.
- **Engine → UI:** `FrameView.onFrame()` copy pixel sang buffer riêng rồi `Platform.runLater(...)`. Nếu JavaFX
  không theo kịp thì frame cũ bị bỏ, engine không bao giờ phải chờ UI.

## 5. Hợp đồng Engine ↔ front-end

Front-end chỉ cần hai thứ:

```java
Engine engine = new Engine(EngineConfig.defaults());   // nạp plugin importer
engine.start(new EngineHost() {                         // khởi động render thread
    public InputState pollInput() { ... }                // gọi mỗi frame, trên render thread
    public void onFrame(ByteBuffer bgra, int w, int h) { ... }  // pixel hàng dưới cùng trước
    public void engineStopped(Throwable error) { ... }   // vòng lặp kết thúc (null = bình thường)
});
engine.loadModel(Path.of("test.obj"));                  // chạy trên thread nền
engine.requestResize(1280, 720);
engine.stop();                                          // chờ render thread giải phóng GPU
```

Vì hợp đồng nhỏ như vậy nên có thể viết front-end khác (ghi video, test render headless) mà không cần sửa engine.

## 6. Tài nguyên GPU

`SceneObject` chỉ giữ `MeshData` và `MaterialData`. Lần đầu một object được vẽ, `GpuResourceCache` sẽ:

- upload mesh → `GpuMesh` (key theo identity của `MeshData`);
- upload mỗi texture → `GpuTexture` (key theo identity, nên texture dùng chung giữa nhiều material chỉ upload một lần;
  texture sRGB dùng `GL_SRGB8_ALPHA8`);
- tạo sampler → `GpuSampler` (key theo giá trị, vì `SamplerData` là record);
- slot texture nào trống thì dùng texture mặc định (checkerboard, normal phẳng, ...).

Tài nguyên sống tới khi `Renderer.close()` được gọi.

## 7. Render pass và trạng thái GL

Hai pass được gọi tuần tự, rõ ràng trong `Renderer.render()`; không có framework "render graph".

**Thứ tự vẽ trong `ScenePass`:** object đục (`OPAQUE`, `MASK`) được vẽ trước theo thứ tự trong scene. Sau đó mới tới object
trong suốt (`BLEND`), sắp từ xa tới gần camera (theo tâm bounding sphere), không ghi depth, để mỗi lớp blend đè lên
những gì nằm phía sau nó.

**Alpha trong shader** (`resolveAlpha` trong `default.frag` / `disney_brdf.frag`): `MASK` bỏ (discard) fragment có alpha
của base color texture nhỏ hơn `uAlphaCutoff`; `BLEND` giữ alpha của texture; `OPAQUE` luôn là 1.

**Màu:** scene shader xuất màu **tuyến tính** (HDR). Tone mapping và gamma chỉ được áp **một lần**, ở `screen_post.frag`.
Quy tắc: **pass nào đổi trạng thái GL thì phải trả lại khi xong.** `ScenePass` tắt blending, bật lại depth write
và gỡ các sampler object ở cuối pass. Thiếu bước này, sampler có mipmap của một material glTF sẽ "rò" sang
`PostProcessPass` và làm cả khung hình bị đen.

Uniform mà mọi scene shader nhận được:

| Uniform | Nội dung |
| --- | --- |
| `uProjection`, `uView`, `uModel`, `uCameraPos` | Camera và object |
| `uLightCount`, `uLightType[i]`, `uLightColor[i]`, `uLightPosition[i]`, `uLightDirection[i]`, `uLightParams[i]` | Tối đa 8 đèn; `params = (intensity, range, cosInner, cosOuter)` |
| `uBaseColor`, `uAlphaMode`, `uAlphaCutoff` | Material |
| `uBaseColorTex`… `uEmissiveTex` (unit 0–4), `u…TexCoord` | Texture và UV set (0/1) của từng slot |

## 8. Mở rộng

- **Thêm scene shader:** tạo `shaders/<tên>.vert` + `.frag` trong `engine/src/main/resources`, thêm tên vào
  `ShaderLibrary.SCENE_SHADERS`. Shader sẽ tự xuất hiện trong combo box của UI.
- **Thêm hiệu ứng post-process:** thêm field vào `PostProcessSettings` (và vào `copy()`, test sẽ báo nếu quên),
  set uniform trong `PostProcessPass.setEffectUniforms`, viết code GLSL trong `screen_post.frag`, thêm một dòng
  `effect(...)` trong `ControlPanel.postProcessSection`.
- **Thêm pass:** tạo class `XxxPass` trong `render` (có `render(...)` và `close()`), tạo `RenderTarget` cho nó trong
  `Renderer`, rồi gọi nó trong `Renderer.render()` ở đúng thứ tự.
- **Thêm định dạng model:** tạo plugin mới trong `plugins/`, implement `ModelImporter`, đánh dấu `@Extension`, thêm
  `plugin.properties` và `include` trong `settings.gradle`. Thư viện riêng của plugin được Gradle tự copy vào
  `build/plugin-libs` (task `pluginLibs`) và PF4J nạp từ đó; không cần khai báo gì ở module `ui`.
- **Thêm loại đèn / đổi đèn mặc định:** `Light.defaultRig()`.

## 9. Kiểm thử

- `gradle test`: unit test cho phần CPU (camera, scene, culling, mesh, settings, parse tham số) cùng `ArchitectureTest`,
  và test cho importer glTF (GLB, node lồng nhau, sparse accessor, material, Draco).
- Smoke test app thật: `gradle run --args="--model test.obj --max-frames 300"` sẽ tự thoát sau 300 frame.

## 10. Plugin glTF

| Class | Trách nhiệm |
| --- | --- |
| `GltfDocument` | Đọc `.gltf` / `.glb`, giải mã accessor (kiểu số nguyên / normalized / sparse) và buffer view |
| `GltfModelImporter` | Duyệt cây node của scene mặc định, cộng dồn transform, đọc từng primitive thành `MeshData` |
| `GltfMaterialLoader` | Material PBR, texture, sampler, `alphaMode`, `KHR_materials_unlit` (hiển thị dưới dạng emissive) |
| `DracoDecoder` | Giải nén `KHR_draco_mesh_compression` bằng [Openize.Drako](https://github.com/openize-drako/Openize.Drako-for-Java): bản port Java thuần của Draco (MIT, ~375 KB, không có native code) |

Chưa hỗ trợ: animation, skinning, morph target, và thành phần alpha của `baseColorFactor` (chỉ alpha từ texture được dùng).
