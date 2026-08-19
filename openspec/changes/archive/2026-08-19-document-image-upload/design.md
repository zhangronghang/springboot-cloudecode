## Context

图片上传与备注管理功能已实现，采用三层架构：Controller → Service 接口 → Service 实现，Service 直接注入 `MongoTemplate` 与 `GridFsTemplate`。图片二进制存 MongoDB GridFS，备注存 `image_metadata` collection。行为规格见主 spec `openspec/specs/image-upload/spec.md`；本设计文档记录实现采用的关键技术决策与权衡（动机见 proposal.md - Why）。

## Goals / Non-Goals

**Goals:**
- 记录图片文件的存储方案（GridFS）与元数据存储方案（`image_metadata` collection）
- 记录统一 API 约定（全部 POST、统一响应格式）的决策依据
- 记录分层架构、异常处理与图片替换策略

**Non-Goals:**
- 不引入新的存储引擎或 ORM（保持 MongoTemplate + GridFsTemplate）
- 不改变既有接口契约与字段定义
- 不启用 MyBatis/MySQL（当前无 mapper XML，数据访问全部走 MongoDB）

## Decisions

### 决策 1：图片文件存 GridFS，元数据存 image_metadata
- **选择**：文件二进制存 GridFS（`GridFsTemplate`），元数据存 `image_metadata` collection（`MongoTemplate`）。
- **理由**：GridFS 将大文件分块存储，规避 MongoDB 单文档 16MB 限制并支持流式读写；元数据与文件分离，便于按字段查询与分页。
- **备选**：文件作为 BSON 二进制直接存单个文档——受 16MB 限制、不便于大图；存本地文件系统 + 数据库存路径——引入文件系统运维负担与一致性风险。

### 决策 2：全部接口使用 POST
- **选择**：所有接口（含 list/detail 查询）统一使用 `@PostMapping`。
- **理由**：参数通过 `@RequestBody` JSON 或 `@RequestParam` 表单传递，风格统一；规避 GET 携带 JSON body 的兼容性问题。
- **备选**：查询类用 GET——与统一 POST 约定冲突，分页/筛选参数需走 query string，可读性较差。

### 决策 3：统一响应格式 ApiResponse
- **选择**：所有接口返回 `{code, message, data}`（`ApiResponse`）。
- **理由**：前端可统一处理成功/失败分支，`code` 用 200/400/500 表达业务结果，`message` 提供可读提示。
- **备选**：依赖 HTTP 状态码表达错误——需额外约定映射，业务错误（如「记录不存在」）与传输错误难以区分。

### 决策 4：Service 直接注入 MongoTemplate + GridFsTemplate
- **选择**：Service 层不经过 DAO/Repository，直接注入 `MongoTemplate` 与 `GridFsTemplate`。
- **理由**：功能单一、查询简单，引入 Repository 层属过度设计；直接操作模板减少样板代码。
- **备选**：使用 Spring Data MongoDB Repository 接口——增加抽象层，但当前查询无复用价值。

### 决策 5：全局异常处理
- **选择**：`@ControllerAdvice` + `@ExceptionHandler` 统一捕获 `MaxUploadSizeExceededException`、`IllegalArgumentException` 与通用 `Exception`，返回 `ApiResponse`。
- **理由**：避免在各 Controller/Service 重复 try-catch，统一错误响应格式。

### 决策 6：update 替换图片采用「先存后删」策略
- **选择**：更新图片时，先将新文件写入 GridFS 成功后再删除旧文件。
- **理由**：避免先删旧文件导致新文件上传失败时数据丢失，保证失败时可回退到旧文件。
- **备选**：先删后存——失败会丢失原图，不可接受。

### 决策 7：元数据字段全部 String 类型
- **选择**：`ImageMetadata` 所有自定义字段（含 `uploadTime`、`fileSize`）均为 String。
- **理由**：与既有约定一致，简化序列化与前端处理；`uploadTime` 用 `yyyy-MM-dd HH:mm:ss` 格式化字符串。
- **备选**：`uploadTime` 用 `LocalDateTime`、`fileSize` 用 `long`——需额外 JSON 序列化配置，且与现有 DTO/测试约定不一致。

## Risks / Trade-offs

- [GridFS 读取需加载完整文件] → detail 接口将整个图片读入内存做 Base64，大图时内存开销高；单文件上限 50MB 内可接受，后续如需流式返回可改造。
- [Base64 放大体积] → Base64 编码使传输体积约增加 1/3；适合当前小图场景，大图可改为返回文件 URL。
- [元数据与文件删除非原子] → 删除时先删 GridFS 文件再删元数据，中途失败可能出现「文件已删、元数据残留」；代码对 GridFS 删除异常做吞掉处理以保证元数据仍被清理，属可接受的一致性权衡。
- [无鉴权与访问控制] → 当前接口无认证/授权，任何可访问者均可上传/删除；若对外暴露需补充鉴权。
