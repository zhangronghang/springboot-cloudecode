## Why

现有信息图片元数据仅记录省级和区县级行政区划代码，缺少市级代码，导致前端无法按完整的省、市、区县层级展示和筛选。需要在不迁移历史 MongoDB 数据、不开放行政区更新能力的前提下补齐市级行政区划字段，并明确特殊行政区结构的传值规则。

## What Changes

- 为信息图片元数据新增 `cityCode` 字符串字段，并在各类响应中返回省、市、区县三个行政区划字段。
- **BREAKING**：上传接口新增必填非空参数 `cityCode`；`districtCode` 继续必填，`provinceCode` 改为允许空字符串，以支持直辖市、省直辖县等非普通三级结构。
- 列表查询新增可选 `cityCode` 精确筛选；与 provinceCode、districtCode 及其他已提供条件组成 AND 查询。
- 更新接口继续不声明 provinceCode、cityCode、districtCode，三个字段创建后均不可修改；详情与删除继续仅使用记录 ID。
- 不迁移或补写历史 MongoDB 数据，旧记录缺少 cityCode 时仍可正常查询、查看、更新和删除。
- 同步更新 Swagger、前端联调文档和接口/服务测试，明确必填状态、特殊地区规则及旧数据兼容行为。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `image-upload`：扩展信息图片的行政区元数据、上传参数、列表筛选、不可更新约束和历史数据兼容要求。

## Impact

- 公开 API：`POST /api/information/upload` 和 `POST /api/information/list` 的请求契约，以及列表、详情、上传和更新响应中的元数据字段。
- 代码：ImageMetadata、ImageListRequest、ImageController、ImageService、ImageServiceImpl 及相关 Swagger 注解。
- 测试与文档：Controller 契约测试、Service 单元测试、Swagger 文档测试和 `docs/api/frontend-integration.md`。
- 数据库：不执行历史数据迁移；新记录写入 cityCode，旧记录允许该字段缺失或为 null。
