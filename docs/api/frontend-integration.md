# 信息图片管理接口联调文档

## 文档入口

- Swagger UI：`http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8081/v2/api-docs`
- 服务地址：`http://localhost:8081`

所有接口均使用 `POST`，响应格式一致：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

`code` 为 `200` 表示成功，`400` 表示参数或业务错误，`500` 表示服务端异常。

> 旧的 `/api/images/*` 路径已删除，请全部切换为 `/api/information/*`。

## 元数据字段与时间

- `provinceCode`：省级行政区划代码；新上传时必填，仅校验非空。
- `districtCode`：区县级行政区划代码；新上传时必填，仅校验非空。
- `createTime`：创建时间，格式 `yyyyMMddHHmmss`，创建后不变。
- `uploadTime`：最近一次上传或更新的时间，格式 `yyyyMMddHHmmss`。

首次上传时 `createTime` 与 `uploadTime` 相同。更新元数据或替换文件后，`uploadTime` 会更新。历史数据的两个行政区字段可能为空字符串。

## 1. 上传图片

- 地址：`POST /api/information/upload`
- Content-Type：`multipart/form-data`
- 必填参数：`file`（图片文件）、`title`（标题）、`provinceCode`（省级行政区划代码）、`districtCode`（区县级行政区划代码）
- 可选参数：`description`（描述）、`tags`（英文逗号分隔的标签）、`uploader`（上传者）

```bash
curl -X POST "http://localhost:8081/api/information/upload" \
  -F "file=@D:/images/demo.jpg" \
  -F "title=示例图片" \
  -F "provinceCode=110000" \
  -F "districtCode=110101" \
  -F "description=用于前端联调" \
  -F "tags=风景,旅行" \
  -F "uploader=zhangsan"
```

成功时 `data` 为完整图片元数据，其中 `id` 用于后续查询、更新和删除。

## 2. 分页查询

- 地址：`POST /api/information/list`
- Content-Type：`application/json`
- `page` 默认 `1`，`size` 默认 `10`；`tag` 为模糊匹配，`uploader`、`provinceCode`、`districtCode` 为精确匹配；多个条件同时提供时取交集。

```json
{
  "page": 1,
  "size": 10,
  "tag": "风景",
  "uploader": "zhangsan",
  "provinceCode": "110000",
  "districtCode": "110101"
}
```

成功时 `data` 包含 `total`、`page`、`size` 和 `records`。

## 3. 查询图片详情

- 地址：`POST /api/information/detail`
- Content-Type：`application/json`

```json
{
  "id": "66a1b2c3d4e5f67890123456"
}
```

成功时 `data` 包含图片元数据和 `imageBase64` 图片内容。

## 4. 更新图片

- 地址：`POST /api/information/update`
- Content-Type：`multipart/form-data`
- 必填参数：`id`
- 可选参数：`file`、`title`、`description`、`tags`、`uploader`。仅更新实际提交的非空字段；`provinceCode` 和 `districtCode` 不属于更新接口参数，无法通过该接口修改。

```bash
curl -X POST "http://localhost:8081/api/information/update" \
  -F "id=66a1b2c3d4e5f67890123456" \
  -F "title=更新后的标题" \
  -F "tags=风景,夏日"
```

如需替换图片，在请求中额外提交 `file`。更新成功或替换图片成功时，`uploadTime` 更新为当前时间，`createTime` 保持不变。

## 5. 删除图片

- 地址：`POST /api/information/delete`
- Content-Type：`application/json`

```json
{
  "id": "66a1b2c3d4e5f67890123456"
}
```

成功时 `data` 为 `null`；系统同时删除 GridFS 中的文件及对应元数据。
