# 足迹图片接口联调文档

## 约定与限制

- 服务地址：`http://localhost:8081`；Swagger：`/swagger-ui/index.html`；OpenAPI：`/v2/api-docs`。
- 业务 POST 响应统一为 `{ "code": 200, "message": "success", "data": {} }`。二进制图片 GET 不使用该包装。
- 仅支持实际可解码、声明类型一致的 JPEG、PNG。单文件上限 50MB，请求总上限 55MB；不限制宽高、像素或单足迹累计容量；每个足迹最多 50 张图片。
- 旧 `/api/images/*` 已废弃，不得调用。图片 URL 为公开、永久、相对地址。

## 创建足迹

`POST /api/information/upload`，`multipart/form-data`。必须包含 `title` 和**恰好一个** `file`；可选字段为 `provinceCode`、`cityCode`、`districtCode`、`description`、`tags`、`uploader`。三个行政区字段独立去空白；未给、空白均保存为 `""`。

```bash
curl -X POST http://localhost:8081/api/information/upload \
  -F "file=@D:/images/footprint.jpg" -F "title=西湖" \
  -F "tags=风景,旅行" -F "uploader=zhangsan"
```

成功时 `data` 为公开足迹记录，包含 `id`、`imageCount: 1` 及首图 `coverImage`。`coverImage.thumbnailUrl` 用于展示，`originalUrl` 用于点击查看原图。

## 查询足迹

- `POST /api/information/list`：接受 `page`、`size`、`tag`、`uploader`、`provinceCode`、`cityCode`、`districtCode`。返回的每条 record 仅包含 `imageCount` 与第一张 `coverImage`，不读取或返回图片 Base64。
- `POST /api/information/detail`：请求 `{ "id": "<informationId>" }`，仅返回足迹公开元数据与 `imageCount`；图片列表应改用下一节接口。
- `POST /api/information/update`：`multipart/form-data`，必填 `id`，可选 `title`、`description`、`tags`、`uploader`。不支持 file、行政区字段或图片替换；图片集合保持不变。
- `POST /api/information/delete`：请求 `{ "id": "<informationId>" }`，删除足迹并尝试清理其所有图片文件。

## 图片管理

### 新增一张

`POST /api/information/image/add`，`multipart/form-data`，包含 `informationId` 和**恰好一个** JPEG/PNG `file`。每次只能增加一张；达到 50 张会返回 `code=400`。

```bash
curl -X POST http://localhost:8081/api/information/image/add \
  -F "informationId=<informationId>" -F "file=@D:/images/second.png"
```

响应中的公开图片对象：

```json
{
  "imageId": "<stable-business-image-id>",
  "fileName": "second.png",
  "fileSize": 12345,
  "contentType": "image/png",
  "width": 1280,
  "height": 720,
  "thumbnailUrl": "/api/information/<informationId>/images/<imageId>/thumbnail",
  "originalUrl": "/api/information/<informationId>/images/<imageId>/original"
}
```

`imageId` 是前端稳定业务 ID；删除接口不得传入 GridFS ID。

### 批量删除

`POST /api/information/image/delete`，JSON：

```json
{
  "informationId": "<informationId>",
  "imageIds": ["<imageId-1>", "<imageId-2>"]
}
```

重复、无效、已删除或不属于该足迹的 ID 不会导致整体失败，响应会报告 `requestedCount`、`deletedCount`、`ignoredImageIds`、`remainingCount`。允许删除最后一张图片，足迹仍保留。

### 图片分页

`POST /api/information/image/list`，JSON：

```json
{ "informationId": "<informationId>", "page": 1, "size": 10 }
```

按图片添加顺序返回 `total`、`page`、`size`、`records`；每项为上面的公开图片对象。没有图片时返回 `code=200`、`message="该足迹没有图片"`、`total=0` 与空 records。

## 浏览器访问图片

- 缩略图：`GET /api/information/{informationId}/images/{imageId}/thumbnail`
- 原图：`GET /api/information/{informationId}/images/{imageId}/original`

两个接口返回 JPEG 或 PNG 二进制而非 JSON，携带长期 `Cache-Control: public, max-age=31536000, immutable` 与 `X-Content-Type-Options: nosniff`。原图还包含 `Content-Disposition: inline`。图片不属于该足迹、已删除或文件缺失时返回 HTTP 404。

## 发布前历史数据清理

该版本不兼容旧单图记录。维护窗口内先停止旧版本写入，运行清理脚本预览并核对范围；正式执行只能删除历史 `image_metadata` 引用的 GridFS 文件及所有历史 `image_metadata`，不得清空共享 bucket。完成后再部署新版本并执行创建、列表、详情、新增、删除、分页和两种图片访问冒烟测试。
