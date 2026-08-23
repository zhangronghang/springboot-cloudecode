## Why

现有图片接口以 `/api/images` 命名，且元数据缺少行政区信息和明确的创建时间，无法满足前端按省、区县联调和后续数据管理需求。需要统一迁移接口命名并规范时间字段，同时为已有 MongoDB 数据提供可控的手工迁移方案。

## What Changes

- **BREAKING**：将 upload、list、detail、update、delete 全部接口从 `/api/images/*` 迁移到 `/api/information/*`，删除旧路径，不提供兼容映射。
- 上传接口新增必填字符串字段 `provinceCode`（省级行政区划代码）和 `districtCode`（区县级行政区划代码）；仅校验非空，不校验六位数字格式。
- 元数据新增 `provinceCode`、`districtCode` 和 `createTime` 字段；`createTime` 与 `uploadTime` 均使用 `yyyyMMddHHmmss` 格式。
- 首次上传时 `createTime` 与 `uploadTime` 取同一当前时间；任意元数据更新或图片替换均将 `uploadTime` 更新为当前时间，`createTime` 保持不变。
- 更新接口不允许修改 `provinceCode` 和 `districtCode`。
- 列表接口支持按 `provinceCode`、`districtCode` 精确筛选，并继续支持既有分页、标签和上传者筛选。
- 同步更新 Swagger 注解、自动生成的接口描述以及前端联调文档。
- 提供幂等的 MongoDB 数据迁移脚本，由用户手工执行：转换历史 `uploadTime`，以转换前的历史上传时间回填 `createTime`，并将历史行政区字段留为空字符串；无法转换的异常时间记录须报告且不得静默改写。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `image-upload`：修改整组接口路径、上传必填字段、列表筛选能力、元数据字段与时间语义，并增加历史数据迁移要求。

## Impact

- API：全部图片管理端点发生不兼容路径变更，上传与列表请求结构变化，响应元数据增加字段。
- 代码：Controller、Service 接口与实现、`ImageMetadata`、`ImageListRequest`、Swagger 注解及相关测试。
- 数据：MongoDB `image_metadata` collection 需要手工执行迁移脚本；GridFS 文件本身不变。
- 文档：`docs/api/frontend-integration.md` 及 Swagger 展示内容需要同步更新。
