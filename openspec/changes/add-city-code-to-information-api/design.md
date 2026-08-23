## Context

当前信息管理接口已经在元数据、上传和列表查询中使用 provinceCode 与 districtCode，并将两个字段排除在更新接口之外；具体新增行为见 `specs/image-upload/spec.md`，变更动机见 `proposal.md`。MongoDB 文档没有固定 schema，历史记录可能缺少新字段；本次明确不扫描、不迁移、不补写历史数据。

## Goals / Non-Goals

**Goals:**

- 在现有 Controller → Service → MongoTemplate 流程中补充 cityCode，保持三个行政区划字段的命名和返回结构一致。
- 允许特殊行政区结构上传空 provinceCode，同时要求新记录具有非空 cityCode 与 districtCode。
- 保持行政区字段创建后不可修改，并让缺少 cityCode 的旧记录继续参与原有操作。
- 让 Swagger、联调文档和自动化测试准确表达新的请求契约。

**Non-Goals:**

- 不迁移、补写或清洗 MongoDB 历史记录，也不修改现有迁移脚本。
- 不校验行政区划代码的数字格式、长度、真实性或上下级关系。
- 不引入行政区划数据源、索引、嵌套地区对象或新的接口路径。
- 不让更新或删除接口以行政区划字段作为输入条件。

## Decisions

### 1. 只扩展缺失的 cityCode 字段

在 ImageMetadata 与 ImageListRequest 中新增 cityCode 及手写 getter/setter，复用已有 provinceCode、districtCode。MongoDB 中已经存在的任一字段均保持原值；应用不会运行字段存在性扫描或后台补写。选择扁平字符串字段是为了保持现有响应和 MongoTemplate 查询方式不变；替代方案是引入嵌套 region 对象，但会扩大接口破坏面。

### 2. 上传时省级代码可空，市级和区县级代码必填

上传 Controller 将 provinceCode 调整为可选参数，并新增必填 cityCode；Service 对省级代码使用“null/空白转空字符串”，对 cityCode、districtCode 执行非空校验，三个值均在保存前 trim。后端不尝试识别直辖市或省直辖县，特殊结构传值的正确性由调用方负责。替代方案是接入行政区划表进行层级校验，但不在本次范围内。

### 3. cityCode 沿用精确 AND 查询语义

列表请求增加可选 cityCode。Service 仅对非空白值追加精确 Criteria；它与 provinceCode、districtCode、tag、uploader 等现有条件共同组成 AND。空白值不生成条件，使 provinceCode 为空的特殊地区不会改变“未筛选省级”的默认语义。暂不增加索引，后续根据数据量和查询性能单独评估。

### 4. 行政区字段保持不可更新，详情和删除仍只认 ID

更新 Controller 与 Service 方法不新增三个行政区参数，持久化更新逻辑也不赋值这些字段。Spring 收到未声明的额外 multipart 参数时不会将其写入元数据，响应中的行政区字段来自数据库原值。详情与删除 DTO 继续仅包含 id，避免改变定位和删除语义。

### 5. 通过缺省映射兼容旧 MongoDB 文档

历史文档缺少 cityCode 时，映射后的字段允许为 null；列表、详情、更新和删除逻辑不得以 cityCode 非空为前置条件。此次不修改现有迁移脚本，也不主动把缺失字段补为空字符串。若历史文档已经存在 cityCode，读取时直接保留其值。

### 6. 将 Swagger、联调文档和测试作为接口契约

Swagger 将 provinceCode 标记为可选，将 cityCode、districtCode 标记为必填，并为列表模型增加 cityCode 精确筛选说明。联调文档提供普通地区和空 provinceCode 的特殊地区示例，提示旧记录 cityCode 可能为 null。测试按先失败后实现的顺序覆盖 Controller 参数、Service 持久化/查询、不可更新、旧数据兼容和 Swagger 文档。

## Risks / Trade-offs

- [旧前端未传 cityCode 时上传失败] → 后端与前端在同一发布窗口切换，并提前交付更新后的 Swagger 与联调文档。
- [允许空 provinceCode 会使普通地区也可提交空值] → 接受由前端保证地区结构正确，本次不引入行政区数据源。
- [不校验代码层级可能保存不一致组合] → 在文档中明确后端只做非空校验，真实性由调用方负责。
- [旧记录 cityCode 为 null 或缺失导致前端显示异常] → 联调文档要求前端兼容 null/缺失值，后端操作不得依赖该字段非空。
- [新增查询条件在大数据量下性能下降] → 先沿用现有无索引策略，出现性能证据后再评估复合索引。

## Migration Plan

1. 先发布已适配 cityCode 的前端或与后端同步发布，确保新上传请求携带 cityCode。
2. 部署后端后通过 Swagger 验证普通地区上传、空 provinceCode 上传、cityCode 查询及原有详情/更新/删除。
3. 不执行数据库脚本；历史记录保持原样。
4. 回滚时仅回滚应用版本；新版本写入的 cityCode 作为 MongoDB 额外字段保留，不影响旧版本读取其他字段。
