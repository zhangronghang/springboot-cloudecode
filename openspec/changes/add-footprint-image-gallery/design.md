## Context

参见 `proposal.md` 的变更动机，以及 `specs/image-upload/spec.md` 的完整行为契约。

当前实现由单个 `ImageController`、`ImageService` 和 `ImageServiceImpl` 处理创建、列表、详情、修改、删除及 GridFS 读取；`ImageMetadata` 直接保存一个 `gridFsFileId`，详情会把整张原图读入内存并编码为 Base64。MongoDB 使用 `MongoTemplate`，图片二进制使用默认 `GridFsTemplate`，没有 DAO/Repository 层，也没有安全认证模块。Spring Boot 2.3.6 和 Java 8 是既定运行环境。

本次变化同时修改公开 API、持久化结构、图片处理和发布数据，因此必须把足迹元数据、图片生命周期和二进制读取分离。业务 POST 接口继续遵循 `ApiResponse` 约定；浏览器直接使用的图片地址是该项目首次引入的 GET 二进制接口。

## Goals / Non-Goals

**Goals:**

- 在不引入新的持久化数据库或 Repository 层的前提下，将每条足迹建模为最多 50 项的有序图片集合。
- 让列表和元数据详情只处理 Mongo 元数据，不读取 GridFS 图片内容。
- 让一张逻辑图片以稳定业务 ID 管理一份原图和一份缩略图，并隐藏内部文件 ID。
- 使用 Mongo 原子数组更新和显式补偿，尽量避免并发越界、丢失更新和半完成文件。
- 将缩略图处理封装为独立组件，使格式校验、EXIF 和缩放逻辑可单独测试。
- 提供可审计、默认只预览的历史数据清理脚本和明确的维护窗口发布顺序。

**Non-Goals:**

- 不为 Mongo 元数据与 GridFS 引入分布式事务或副本集事务前提。
- 不增加图片排序、替换、去重、鉴权、签名 URL、累计容量配额或分辨率限制。
- 不保留旧单图文档的运行时读取兼容层。
- 不重构与本功能无关的 MyBatis、MySQL 或项目其他模块。

## Decisions

### 1. 图片元数据内嵌在足迹文档

`ImageMetadata` 增加 `List<ImageItem> images`，每个 `ImageItem` 包含：

```text
imageId
originalGridFsFileId
thumbnailGridFsFileId
fileName
fileSize (long)
contentType
width
height
createTime
```

删除足迹级的 `gridFsFileId`、`fileName`、`fileSize`。数组位置表达顺序，第一项即封面；`imageCount` 在 DTO 组装时根据数组长度计算，不写入 Mongo。

选择内嵌数组是因为每条足迹硬限制 50 张，数组大小有界，列表可直接取得封面且新增、删除可以使用 Mongo 原子数组操作。备选方案是独立 `information_image` 集合，它更适合无上限图片量，但会增加封面查询和跨集合一致性成本；直接使用 GridFS metadata 则会把业务查询、原缩略图配对和 API 身份强耦合到存储实现。

### 2. 业务 imageId 与 GridFS ID 分离

每张图片生成独立 ObjectId 字符串作为 `imageId`，原图和缩略图各自拥有内部 GridFS ObjectId。公开 DTO、删除请求和永久 URL 只使用 `imageId`。后端通过 `informationId + imageId` 找到内嵌条目并解析实际文件 ID。

相比直接公开原图 GridFS ID，该方案多存一个很小的字段，但一张逻辑图片可以稳定绑定两个物理文件；重新生成缩略图或未来迁移文件存储时不改变公开身份，也不会允许删除 API 接收任意内部文件 ID。

### 3. 分离足迹服务、图片服务和资源读取

保留现有三层风格，但按职责拆分：

- `ImageService`/实现：创建、列表、元数据详情、元数据修改、删除整条足迹。
- `InformationImageService`/实现：单张新增、批量删除、图片分页、通过归属关系解析图片资源。
- `ThumbnailService`/实现：识别 JPEG/PNG、解码、读取尺寸和 EXIF、生成缩略图。
- `ImageController`：业务 POST 接口。
- `ImageResourceController`：两个 GET 二进制接口。
- 响应 DTO 组装器或私有映射方法：构造 `imageCount`、`coverImage` 和永久相对 URL，避免直接序列化持久化实体。

不新增 Repository 层，两个 Service 继续直接使用 `MongoTemplate` 和 `GridFsTemplate`，与项目既有架构保持一致。

### 4. 使用 Java 图片解码能力并补充 EXIF 读取能力

`ThumbnailService` 使用 Java 8 可用的 `ImageIO`/Java2D 完成 JPEG、PNG 解码与等比例缩放，并引入一个兼容 Java 8 的 EXIF metadata 读取依赖来解析 JPEG Orientation。格式判定同时检查文件签名、可用 ImageReader 和实际解码结果，不信任扩展名或 multipart Content-Type。

缩放框固定为 `320×320`，不裁剪、不放大小图。JPEG 按修正后的方向输出 JPEG；PNG 使用支持 alpha 的缓冲区并输出 PNG。原图使用上传字节原样存储。

备选方案是引入完整图像处理服务或原生库，可提供更强的资源隔离和格式支持，但部署复杂度与当前只支持 JPEG/PNG 的范围不匹配。只使用 `ImageIO` 而不解析 EXIF 会导致常见手机竖图缩略图方向错误。

### 5. 创建和单张新增采用文件补偿

创建或新增的处理顺序为：

```text
验证请求仅有一个文件
  → 解码并生成缩略图数据
  → 生成业务 imageId
  → 写入原图 GridFS
  → 写入缩略图 GridFS
  → 保存/原子追加元数据
  → 返回公开 DTO
```

每一步记录本次已经生成的 GridFS ID。后续步骤失败时，在 catch/finally 补偿删除这些文件。文件尚未写入 Mongo 元数据前不会被公开地址发现。

创建足迹保存一个只含初始 `ImageItem` 的新文档。单张新增通过带 `_id=informationId` 且 `images.49` 不存在的查询执行原子 `$push`，同时设置足迹 `uploadTime`。这样并发请求最多只有会使数组保持在 50 项以内的更新成功；条件未匹配时区分足迹不存在和已达上限，并清理本次两个文件。

选择补偿而不是跨 Mongo/GridFS 事务，是因为当前部署没有声明副本集事务条件，且 GridFS 文件与元数据的失败处理可以在服务边界内清晰完成。

### 6. 批量删除使用原子 pull，文件清理在后

删除请求先对 imageIds 保序去重。Service 使用同时匹配足迹 ID 和至少一个目标图片的原子 `findAndModify/$pull`，取得实际被移除的旧图片条目并更新 `uploadTime`；若没有目标匹配，则确认足迹是否存在并返回全部 ID 被忽略。`requestedCount` 使用去重后的数量，`deletedCount` 使用实际移除数量，`ignoredImageIds` 保持请求去重后的相对顺序。

元数据移除成功后删除各条目的原图和缩略图。文件已经不存在视为清理完成；其他删除异常记录足迹 ID、业务 imageId 和内部文件 ID，不把已删除元数据写回。删除后重新读取或计算当前图片数量作为 `remainingCount`；该值表示请求完成时的最新可见结果，但与随后发生的并发新增或删除之间不提供快照保证。

删除整条足迹使用同一原则：先取得并删除足迹元数据，再清理它引用的所有文件。相比先删文件，这能避免一个仍可查询的足迹指向已经主动删除的图片。

### 7. 查询只返回 DTO 和相对永久地址

列表查询仍按既有筛选、分页和 uploadTime 排序读取 `image_metadata`。DTO 仅从数组长度和第一项构造 `imageCount`、`coverImage`，不调用 GridFS。详情同样只组装公开元数据和 imageCount。

图片分页最多面对 50 项，因此读取单条足迹的图片元数据数组后在服务内按 page/size 切片即可；不会读取任何图片字节。相比单独集合分页，这避免为小型有界数组引入额外集合与索引。

URL 使用相对路径：

```text
/api/information/{informationId}/images/{imageId}/thumbnail
/api/information/{informationId}/images/{imageId}/original
```

不持久化 URL，也不拼接部署域名，避免反向代理、端口或域名变更造成数据更新。

### 8. 二进制接口验证归属并流式返回

资源 Controller 先通过 Mongo 查询验证 `informationId + imageId`，再选择内部原图或缩略图 ID，从 GridFS 获取 Resource 并流式写入响应。响应使用持久化的实际 `contentType`，原图设置 inline，二者均设置：

```http
Cache-Control: public, max-age=31536000, immutable
X-Content-Type-Options: nosniff
```

业务 imageId 永不复用、图片不可替换，因此长期缓存不会把同一 URL 映射到不同内容。找不到足迹、图片归属或 GridFS 文件统一返回 HTTP 404，避免泄露图片是否属于其他足迹。该 Controller 不走 `ApiResponse` 的 JSON 异常包装，需要局部异常映射或直接构造 `ResponseEntity`。

### 9. 上传配置和错误映射

配置修改为 `max-file-size=50MB`、`max-request-size=55MB`。55MB 请求上限只用于容纳一个 50MB 文件之外的 multipart 边界和文本字段，不授权多文件新增。

普通业务接口继续返回 `ApiResponse` code 400/500。现有 `MaxUploadSizeExceededException` 映射继续返回“上传文件大小超过限制”。图片格式、文件数量、足迹不存在、50 张上限等返回业务 code 400；无法补偿的存储错误返回 code 500 并记录内部详情。

### 10. 历史清理脚本默认预览并定向删除

在 `scripts/mongodb` 增加一次性脚本及脚本测试。脚本默认只预览；只有明确的执行参数才进入删除模式。它先扫描全部历史 `image_metadata`，保存足迹 ID 和非空旧 `gridFsFileId` 清单，然后只按该清单删除 GridFS 文件，最后删除全部 `image_metadata`。不得对 `fs.files` 或 `fs.chunks` 执行无条件删除。

脚本输出足迹数、唯一关联文件数、成功数、已缺失数、失败数及失败 ID。即使部分文件已缺失，也可继续删除历史足迹；其他删除失败 ID 保留在报告中供人工清理。执行前必须停止旧版本写入，否则扫描后创建的数据可能逃逸清理。

## Risks / Trade-offs

- [不限制图片总像素，压缩后较小的超高分辨率图片可能消耗大量堆内存甚至触发 OOM] → 保留 50MB 字节限制、将图片处理封装并记录指标/异常；该风险无法在不增加像素限制或进程隔离的情况下彻底消除，已由需求方接受。
- [每条足迹最多可保存约 2.5GB 原图，系统总存储没有业务配额] → 通过 50 张硬上限限制元数据规模，并由部署侧监控 Mongo/GridFS 容量；本次不增加累计字节配额。
- [Mongo 元数据与 GridFS 缺少统一事务，进程在补偿前退出可能留下孤立文件] → 正常异常路径立即补偿，删除失败记录完整内部 ID，并保留后续孤立文件清理能力。
- [公开永久 URL 允许任何获得地址的人读取图片] → 使用不可复用的业务 ObjectId 并验证足迹归属，但不把不可猜测 ID 当作鉴权；公开访问是已确认的产品选择。
- [长期浏览器缓存可能在服务端删除后继续展示客户端副本] → 前端删除成功后移除 URL 引用；由于 URL 永不复用，不会显示成另一张图片。
- [引入 EXIF 读取依赖增加供应链和维护成本] → 只选择兼容 Java 8、用途单一的成熟依赖，并用方向样本测试锁定行为。
- [公开 API 和数据结构均不向后兼容] → 在维护窗口停写并先清理旧数据，同一发布窗口切换前后端；更新 Swagger 和联调文档。
- [历史清理不可逆] → 脚本默认预览并打印精确范围；如需要回滚旧版本，运维必须在清理前自行保留 MongoDB/GridFS 备份，否则无法恢复旧足迹。

## Migration Plan

1. 在非生产数据上运行清理脚本测试，验证只删除 `image_metadata` 引用的文件。
2. 部署前构建并验证新后端与前端契约，但不启动新版本接收流量。
3. 进入维护窗口，停止旧版本写入。
4. 以预览模式执行脚本，核对足迹数和关联文件数；如需可回滚发布，在此时完成外部备份。
5. 以正式模式执行脚本，核对文件成功、缺失、失败 ID 和足迹删除数量。
6. 部署新版本并执行创建、列表、详情、单张新增、批量删除、图片分页、缩略图与原图访问冒烟测试。
7. 切换前端到新契约并结束维护窗口。

应用回滚到旧版本只有在恢复清理前 MongoDB/GridFS 备份后才安全；没有备份时不得回滚到依赖旧单图结构的版本。若新版本启动失败但不回滚数据，可保持维护状态、修复后重新部署，因为数据库已为空且新结构尚未产生或只产生新结构数据。
