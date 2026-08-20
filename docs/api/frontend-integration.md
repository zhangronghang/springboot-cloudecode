# 图片管理接口联调文档

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

## 1. 上传图片

- 地址：`POST /api/images/upload`
- Content-Type：`multipart/form-data`
- 必填参数：`file`（图片文件）、`title`（标题）
- 可选参数：`description`（描述）、`tags`（英文逗号分隔的标签）、`uploader`（上传者）

```bash
curl -X POST "http://localhost:8081/api/images/upload" \
  -F "file=@D:/images/demo.jpg" \
  -F "title=示例图片" \
  -F "description=用于前端联调" \
  -F "tags=风景,旅行" \
  -F "uploader=zhangsan"
```

成功时 `data` 为完整图片元数据，其中 `id` 用于后续查询、更新和删除。

## 2. 分页查询

- 地址：`POST /api/images/list`
- Content-Type：`application/json`
- `page` 默认 `1`，`size` 默认 `10`；`tag` 为模糊匹配，`uploader` 为精确匹配。

```json
{
  "page": 1,
  "size": 10,
  "tag": "风景",
  "uploader": "zhangsan"
}
```

成功时 `data` 包含 `total`、`page`、`size` 和 `records`。

## 3. 查询图片详情

- 地址：`POST /api/images/detail`
- Content-Type：`application/json`

```json
{
  "id": "66a1b2c3d4e5f67890123456"
}
```

成功时 `data` 包含图片元数据和 `imageBase64` 图片内容。

## 4. 更新图片

- 地址：`POST /api/images/update`
- Content-Type：`multipart/form-data`
- 必填参数：`id`
- 可选参数：`file`、`title`、`description`、`tags`、`uploader`。仅更新实际提交的字段。

```bash
curl -X POST "http://localhost:8081/api/images/update" \
  -F "id=66a1b2c3d4e5f67890123456" \
  -F "title=更新后的标题" \
  -F "tags=风景,夏日"
```

如需替换图片，在请求中额外提交 `file`。

## 5. 删除图片

- 地址：`POST /api/images/delete`
- Content-Type：`application/json`

```json
{
  "id": "66a1b2c3d4e5f67890123456"
}
```

成功时 `data` 为 `null`；系统同时删除 GridFS 中的文件及对应元数据。
