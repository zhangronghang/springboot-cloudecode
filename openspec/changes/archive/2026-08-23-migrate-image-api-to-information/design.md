## Context

当前图片模块以单个 Controller 和 Service 处理 GridFS 文件与 `image_metadata` 元数据，时间使用字符串 `yyyy-MM-dd HH:mm:ss`，接口统一挂载在 `/api/images`。本次变更同时影响公开 API、请求 DTO、持久化模型、查询条件、时间语义、历史数据和联调文档；动机见 `proposal.md`，行为契约见 `specs/image-upload/spec.md`。

项目使用 Java 8 和 Spring Boot 2.3.6，继续沿用字符串时间与 MongoTemplate/GridFsTemplate，不引入新的数据库或日期依赖。历史数据迁移由用户在部署窗口内手工执行，应用启动时不得自动修改存量数据。

## Goals / Non-Goals

**Goals:**

- 以一次明确的破坏性升级完成整组接口迁移，不保留容易造成双写或长期兼容负担的旧映射。
- 保证新记录的创建时间稳定不变，并用 uploadTime 表达最近一次有效更新或图片替换时间。
- 让行政区筛选与现有 MongoDB 查询方式保持一致，并让 Swagger、联调文档和代码契约同步。
- 提供可预演、可重复执行、会报告异常记录的历史数据迁移脚本。

**Non-Goals:**

- 不校验行政区划代码是否为六位数字，也不校验省与区县之间的隶属关系。
- 不允许通过更新接口修正 provinceCode 或 districtCode；历史空值的后续补录不在本次 API 范围内。
- 不改变 MongoDB collection 名称、GridFS 存储结构、统一响应格式或接口统一使用 POST 的约定。
- 不自动执行迁移脚本，也不在应用启动阶段扫描或修复历史数据。

## Decisions

### 1. 直接替换 Controller 基础路径

将现有基础映射从 `/api/images` 改为 `/api/information`，保留 upload、list、detail、update、delete 子路径和请求方式。不会额外声明旧基础路径或重定向。

选择该方式是因为用户明确要求删除旧接口；同时暴露新旧路径会延长不兼容窗口，也无法验证前端已真正完成迁移。替代方案是短期双路径兼容，但与“不保留旧路径”的要求冲突。

### 2. 在现有领域对象和方法签名上扩展字段

`ImageMetadata` 增加字符串属性 provinceCode、districtCode、createTime；上传 Service 方法增加两个行政区参数；`ImageListRequest` 增加两个可选筛选字段。更新接口与更新 Service 方法不增加行政区参数，从公开签名上限制这两个字段不可更新。

选择继续使用字符串字段，是为了与现有实体、MongoDB 文档和接口响应保持一致。替代方案是引入新的上传 DTO 或行政区值对象，但会扩大本次改动且不能带来额外校验价值。

### 3. 使用统一格式化器并按一次请求捕获时间

使用线程安全的 `DateTimeFormatter` 和格式 `yyyyMMddHHmmss`。首次上传只获取并格式化一次当前时间，然后同时赋给 createTime 与 uploadTime，避免跨秒导致两个字段不一致。

更新时先判断是否存在至少一个非空可更新字段或有效替换文件；更新成功后再刷新 uploadTime，createTime 永不赋新值。文件存储失败时直接返回错误，不刷新 uploadTime。替代方案是每个字段更新时独立取时间，但会产生不一致语义，也难以测试。

### 4. 行政区筛选使用精确条件并按 AND 组合

列表查询对非空 provinceCode、districtCode 使用精确匹配，与 uploader 的处理一致；与 tag、uploader 等已有条件共同加入同一个查询，因此自然形成 AND 关系。继续按 uploadTime 字符串降序排序；固定十四位格式的字典序与时间顺序一致。

不创建正则校验或行政区索引。当前数据量和性能需求未要求新增索引；若上线后查询量增长，可单独评估复合索引。

### 5. 使用独立的 mongosh 幂等迁移脚本

在 `scripts/mongodb/` 下提供迁移脚本和执行说明。脚本以 `image_metadata` 为目标逐条检查：

- uploadTime 符合旧格式时，转换为十四位新格式；符合新格式时保留。
- createTime 缺失或为空时，使用规范化后的 uploadTime 回填；已有值不覆盖。
- provinceCode、districtCode 仅在字段缺失时回填空字符串，已有值不覆盖。
- uploadTime 无法识别时输出记录 id 与原值，并跳过该记录的全部修改。
- 默认先执行 dry-run，仅输出计划与汇总；用户显式关闭 dry-run 后才写入。

逐条更新比一次聚合管道更易兼容未知的 MongoDB 版本，也能精确报告异常记录。替代方案是应用启动迁移或批量 updateMany；前者违背手工执行要求，后者难以对异常时间进行逐条报告和安全跳过。

### 6. 同步测试和接口文档作为契约的一部分

Service 单元测试覆盖上传必填校验、时间相等与格式、行政区查询条件、更新时的时间变化及 createTime 不变；Controller 层增加路径与参数契约验证，确认旧路径不存在。Swagger 注解和 `docs/api/frontend-integration.md` 使用新路径、字段和时间示例，并写明历史行政区字段可能为空。

## Risks / Trade-offs

- [旧前端在部署后立即无法调用 `/api/images/*`] → 后端与前端按同一发布窗口切换，并在部署前提供更新后的联调文档。
- [历史数据未迁移时，新旧时间字符串混排会导致排序异常] → 部署前停止写入、完成 dry-run 和正式迁移，再启动新版本。
- [手工脚本可能部分执行或操作错误数据库] → 默认 dry-run、打印数据库与 collection、逐条幂等更新并在执行前备份 collection。
- [秒级时间精度无法区分同一秒内的多次更新] → 接受 `yyyyMMddHHmmss` 指定精度；本次不引入更高精度字段。
- [历史行政区为空时按行政区筛选无法命中] → 文档明确历史数据回填为空字符串；行政区补录另行设计。
- [客户端携带行政区更新参数时可能误以为已修改] → 更新接口签名和 Swagger 均不声明这些参数，响应返回持久化后的原值。

## Migration Plan

1. 对 `claude_code.image_metadata` 建立可恢复备份，并暂停旧版本写入。
2. 在目标数据库运行迁移脚本的 dry-run，核对待更新数量和异常 id；先人工处理或确认跳过异常记录。
3. 关闭 dry-run 后执行正式迁移，再次执行 dry-run/统计检查，确认没有剩余可迁移记录且重复执行不会产生变化。
4. 同一发布窗口部署后端新版本并让前端切换到 `/api/information/*`，使用 Swagger 和联调示例完成冒烟验证。
5. 若需要回滚，停止新版本写入，恢复数据库备份并部署旧版本；GridFS 文件无需迁移或恢复。
