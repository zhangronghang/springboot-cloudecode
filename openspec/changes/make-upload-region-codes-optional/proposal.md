## Why

部分图片在上传时无法立即提供完整的省、市、区县行政区信息，现有 cityCode 和 districtCode 必填规则会阻止这类有效图片入库。需要将三个行政区字段统一为可选字段，使上传接口能够接收不完整或暂缺的行政区信息。

## What Changes

- 将上传接口的 provinceCode、cityCode、districtCode 统一调整为可选参数。
- 三个字段缺失、为空或仅包含空白时统一持久化为 `""`；非空值去除首尾空白后保存。
- 允许三个字段任意组合，不校验字符组成、长度或行政区层级关系。
- 保持 file、title 必填，移除 cityCode、districtCode 缺失时的 400 错误行为。
- 保持列表接口现有语义：非空行政区值精确筛选，空白值忽略，且不支持专门查询空行政区记录。
- 保持更新接口现有语义：三个行政区字段创建后不可修改；详情、删除和历史数据不迁移。
- 同步 Swagger、前端联调文档、OpenSpec 主规范和自动化测试。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `image-upload`：放宽图片上传的行政区字段约束，并定义三个可选字段的规范化、持久化及兼容行为。

## Impact

- 受影响接口：`POST /api/information/upload` 的请求约束、错误响应和 Swagger 描述。
- 受影响代码：ImageController、ImageServiceImpl 及相关契约、Service、Swagger 测试。
- 受影响文档：`docs/api/frontend-integration.md` 与 `openspec/specs/image-upload/spec.md`。
- 不新增依赖，不修改数据库结构，不执行历史数据迁移。
- 兼容性：接口约束被放宽；此前因缺少 cityCode 或 districtCode 返回 400 的请求将改为成功并保存空字符串。
