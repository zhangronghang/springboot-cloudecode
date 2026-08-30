## Why

当前一条信息记录只能关联一张图片，足迹元数据查询还与原图 Base64 读取耦合，无法满足同一足迹后续快速增删图片、列表轻量展示封面和图片分页浏览的需求。需要把单图记录升级为有序图片集合，并为缩略图与原图提供可直接访问的永久地址。

## What Changes

- 创建足迹时仍必须且只能上传一张图片，上传内容严格限定为可解码的 JPEG 或 PNG。
- 一条足迹改为维护最多 50 张、允许为空的有序图片集合，第一张图片作为列表封面。
- 新增单张图片上传接口，以及支持一次删除一张或多张图片的幂等删除接口。
- 足迹列表仅返回第一张图片的缩略图地址和原图地址；无图片时返回空封面状态。
- **BREAKING**：`POST /api/information/detail` 改为只返回足迹元数据和图片数量，不再返回图片 Base64；新增独立的足迹图片分页查询接口。
- **BREAKING**：`POST /api/information/update` 不再接收文件，图片只能通过独立新增、删除接口管理。
- 新增公开、永久、不鉴权的缩略图和原图 GET 地址，直接返回图片二进制。
- 上传时同步生成最大 `320×320` 的等比例缩略图，支持 JPEG EXIF 方向修正和 PNG 透明通道。
- 单文件上限保持 50MB，请求上限调整为 55MB；不限制原图宽高、总像素或单足迹累计图片容量。
- **BREAKING**：删除全部历史足迹记录及其实际关联的历史 GridFS 图片，不迁移、不兼容旧单图元数据结构；共享 GridFS bucket 中的其他文件不得删除。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `image-upload`：将现有单图信息管理契约升级为足迹多图片管理，拆分元数据与图片查询，增加缩略图、永久图片访问、独立图片增删和历史数据清理要求。

## Impact

- 公开 API：修改 upload、update、list、detail、delete 的请求或响应契约，新增图片 add/delete/list 和原图、缩略图访问接口。
- 数据模型：`image_metadata` 从单个 `gridFsFileId/fileName/fileSize` 改为最多 50 项的内嵌 `images` 数组；GridFS 同时保存原图和缩略图。
- 后端代码：影响 Controller、Service 接口与实现、MongoDB 原子数组更新、GridFS 文件生命周期、统一异常处理及响应 DTO。
- 图片处理：需要 JPEG/PNG 内容识别、尺寸读取、EXIF 方向处理和缩略图生成能力。
- 配置与运维：`spring.servlet.multipart.max-request-size` 调整为 55MB，并新增只能在维护窗口手工执行的历史足迹清理脚本。
- 测试与文档：更新 Controller 契约、Service、图片处理、清理脚本、Swagger 和前端联调文档。
