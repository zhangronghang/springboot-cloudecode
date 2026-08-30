## 1. 契约测试与基础配置

- [x] 1.1 扩展 `ImageControllerContractTest`，先覆盖创建仅允许一个 file、update 不再接收 file、三个图片 POST 接口和两个图片 GET 接口，并使用指定 Maven settings 运行该测试确认新契约在实现前失败。
- [x] 1.2 扩展 `SwaggerDocumentationTest`，定义新旧接口参数、永久 URL、二进制响应和 50 张上限的文档断言，并使用指定 Maven settings 运行测试确认断言可用于回归。
- [x] 1.3 在 `pom.xml` 增加兼容 Java 8 的 EXIF metadata 读取依赖，将 `max-request-size` 调整为 55MB、保留 `max-file-size` 50MB，并使用 `mvnw dependency:tree` 与编译验证依赖和配置可加载。

## 2. 图片数据模型与公开 DTO

- [x] 2.1 新增内嵌图片实体并把 `ImageMetadata` 改为有序 `images` 数组，移除足迹级单图字段，使用实体单元测试验证 images 可为空、字段类型正确且 imageCount 未被持久化。
- [x] 2.2 新增封面、公开图片、足迹列表/详情和图片分页 DTO 及映射逻辑，使用序列化测试验证响应包含 imageCount、thumbnailUrl、originalUrl，且不暴露两个 GridFS ID、内部 images 数组或 Base64。
- [x] 2.3 新增图片新增、批量删除和图片分页请求 DTO，使用 DTO/Controller 测试验证 informationId、imageIds、page、size 的绑定和空参数错误。

## 3. JPEG/PNG 校验与缩略图生成

- [x] 3.1 为 `ThumbnailService` 编写 JPEG、PNG、伪造类型、损坏文件、横竖图、小图不放大、PNG 透明通道和 JPEG EXIF Orientation 测试，并使用指定 Maven settings 运行确认测试先失败。
- [x] 3.2 实现文件签名、ImageReader 和实际解码的组合校验，记录原图 contentType、width、height，并运行 `ThumbnailService` 测试验证仅接受可解码 JPEG/PNG 且不检查总像素。
- [x] 3.3 实现最大 320×320 等比例缩略图、JPEG EXIF 方向修正和 PNG alpha 保留，并运行图片处理测试验证格式、尺寸、方向和透明通道。

## 4. 创建足迹与单张新增图片

- [x] 4.1 重写创建足迹服务流程，使其生成业务 imageId、存储原图与缩略图并保存首个 ImageItem，补充 Mockito 测试验证成功数据、时间字段和第一张封面。
- [x] 4.2 为创建流程实现失败补偿，使用 Mockito 测试分别模拟原图存储、缩略图存储和元数据保存失败，验证本次文件被清理且不留下足迹或不完整图片。
- [x] 4.3 修改 upload Controller 与 Service 签名以严格检测且仅接受一个 file，运行 Controller 和 Service 定向测试验证缺文件、多文件、空标题、不支持格式和行政区字段原有规范化行为。
- [x] 4.4 实现 `POST /api/information/image/add` 及服务接口，使用带 `images.49` 不存在条件的原子 `$push` 追加图片并更新 uploadTime，运行单元测试验证空图片足迹、正常追加、足迹不存在和第 51 张被拒绝。
- [x] 4.5 为单张新增实现 GridFS 失败补偿与并发上限测试，验证条件更新未匹配或任一步失败时清理本次原图和缩略图，且并发请求不能使图片数超过 50。

## 5. 足迹和图片查询契约

- [x] 5.1 修改 `/api/information/list` 的响应映射，只根据 images 第一项返回 coverImage 和 imageCount，运行测试验证有图、无图、既有筛选/分页/排序以及查询过程不调用 GridFS。
- [x] 5.2 修改 `/api/information/detail` 为只返回公开元数据和 imageCount，运行测试验证响应不含 imageBase64、内部 images 或 GridFS ID，且服务不读取 GridFS。
- [x] 5.3 从 `/api/information/update` 移除文件替换能力并保持既有非空元数据更新、行政区不可修改和时间规则，运行回归测试验证图片集合始终不变。
- [x] 5.4 实现 `POST /api/information/image/list`，按 images 数组顺序在内存中分页映射公开 DTO，运行测试验证边界页、默认分页、足迹不存在和 code=200/message="该足迹没有图片" 的空状态。

## 6. 图片与足迹删除

- [x] 6.1 为批量图片删除编写服务测试，覆盖保序去重、有效与无效混合 ID、跨足迹 ID、重复删除、删除封面、删除最后一张和空 imageIds，并确认测试在实现前失败。
- [x] 6.2 实现 `POST /api/information/image/delete`，通过原子 findAndModify/$pull 移除实际匹配图片并更新 uploadTime，运行测试验证 requestedCount、deletedCount、ignoredImageIds 和 remainingCount。
- [x] 6.3 在元数据移除后清理每张图片的原图和缩略图，使用 Mockito 测试验证文件缺失不阻断成功、其他删除异常被记录且不会恢复已删除元数据。
- [x] 6.4 修改整条足迹删除流程为先删除元数据再清理 images 中全部文件，运行测试验证多图、空图片集合、文件缺失和清理失败场景。

## 7. 原图和缩略图永久访问

- [x] 7.1 实现通过 informationId+imageId 查询归属并解析内部文件 ID 的资源读取服务，运行单元测试验证正确资源、跨足迹 ID、缺失图片和缺失 GridFS 文件。
- [x] 7.2 实现两个 GET 二进制接口并配置资源专用错误响应，使用 MockMvc 测试验证原图/缩略图字节、Content-Type、原图 inline、HTTP 400/404/500 及无 ApiResponse 包装。
- [x] 7.3 为图片响应增加 `Cache-Control: public, max-age=31536000, immutable` 和 `X-Content-Type-Options: nosniff`，运行资源接口测试验证两个地址的缓存和安全响应头。

## 8. 历史足迹清理脚本

- [x] 8.1 先扩展 MongoDB 脚本测试，构造历史足迹引用、缺失文件和共享 bucket 非足迹文件，验证预览模式不写数据且正式模式只处理足迹引用文件。
- [x] 8.2 实现默认预览、显式执行的一次性历史清理脚本，输出足迹数、唯一关联文件数、成功/缺失/失败文件和失败 ID，再运行脚本测试验证全部历史 `image_metadata` 被删除且共享文件保留。
- [x] 8.3 在脚本说明中记录停写、预览、可选备份、正式执行、核对报告和部署顺序，并通过人工审阅确认没有无条件清空 `fs.files` 或 `fs.chunks` 的语句。

## 9. 文档与完整验证

- [x] 9.1 更新 Swagger 注解和 `docs/api/frontend-integration.md`，加入新请求/响应示例、永久相对 URL、空图片提示、批量删除结果、JPEG/PNG 与 50MB/55MB 限制，并运行 Swagger 测试验证文档。
- [x] 9.2 使用 `.\mvnw.cmd -s 'D:\develop\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\conf\settings.xml' test` 运行全部 Maven 测试，修复所有回归并记录通过结果。
- [x] 9.3 运行 MongoDB 清理脚本测试，并在测试环境完成创建、列表、元数据详情、单张新增、批量删除、图片分页、缩略图和原图访问的冒烟验证，确认响应与 delta spec 一致。
