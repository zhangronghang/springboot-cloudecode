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

- `provinceCode`：省级行政区划代码；新上传时可选。
- `cityCode`：市级行政区划代码；新上传时可选。
- `districtCode`：区县级行政区划代码；新上传时可选。
- `createTime`：创建时间，格式 `yyyyMMddHHmmss`，创建后不变。
- `uploadTime`：最近一次上传或更新的时间，格式 `yyyyMMddHHmmss`。

三个行政区代码独立处理：缺失、空字符串或仅包含空白时保存为 `""`，非空值去除首尾空白后保存；可以任意组合提供，不校验字符组成、长度、数字格式或上下级关系。直辖市、省直辖县等非普通“省—市—区县”结构可以只传实际拥有的字段。首次上传时 `createTime` 与 `uploadTime` 相同；更新元数据或替换文件后，`uploadTime` 会更新。历史记录的任一行政区字段可能缺失或为 `null`，查询详情、更新和删除仍可正常使用。

## 1. 上传图片

- 地址：`POST /api/information/upload`
- Content-Type：`multipart/form-data`
- 必填参数：`file`（图片文件）、`title`（标题）
- 可选参数：`provinceCode`（省级行政区划代码）、`cityCode`（市级行政区划代码）、`districtCode`（区县级行政区划代码）、`description`（描述）、`tags`（英文逗号分隔的标签）、`uploader`（上传者）

普通“省—市—区县”结构示例：

```bash
curl -X POST "http://localhost:8081/api/information/upload" \
  -F "file=@D:/images/demo.jpg" \
  -F "title=示例图片" \
  -F "provinceCode=110000" \
  -F "cityCode=110100" \
  -F "districtCode=110101" \
  -F "description=用于前端联调" \
  -F "tags=风景,旅行" \
  -F "uploader=zhangsan"
```

三个行政区字段全部省略：

```bash
curl -X POST "http://localhost:8081/api/information/upload" \
  -F "file=@D:/images/demo.jpg" \
  -F "title=行政区暂缺示例"
```

仅提供部分行政区字段（字段之间没有依赖关系）：

```bash
curl -X POST "http://localhost:8081/api/information/upload" \
  -F "file=@D:/images/demo.jpg" \
  -F "title=仅区县示例" \
  -F "districtCode=district-only"
```

成功时 `data` 为完整图片元数据，其中 `id` 用于后续查询、更新和删除。新记录中的三个行政区字段均为非 `null` 字符串；例如仅提供上述 districtCode 时：

```json
{
  "provinceCode": "",
  "cityCode": "",
  "districtCode": "district-only"
}
```

缺少行政区字段不会返回 400。上传接口的 400 参数错误仅包括 `file` 为空或 `title` 为空等必填参数问题。与旧版本相比，此前因 cityCode 或 districtCode 缺失、为空或仅含空白而返回 400 的请求，现在会上传成功并将对应字段保存为 `""`。

## 2. 分页查询

- 地址：`POST /api/information/list`
- Content-Type：`application/json`
- `page` 默认 `1`，`size` 默认 `10`；`tag` 为模糊匹配，`uploader`、`provinceCode`、`cityCode`、`districtCode` 为精确匹配；多个条件同时提供时取交集。行政区筛选值缺失或仅含空白时忽略该条件，因此当前不支持专门查询行政区字段为空的记录。

```json
{
  "page": 1,
  "size": 10,
  "tag": "风景",
  "uploader": "zhangsan",
  "provinceCode": "110000",
  "cityCode": "110100",
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
- 可选参数：`file`、`title`、`description`、`tags`、`uploader`。仅更新实际提交的非空字段；`provinceCode`、`cityCode` 和 `districtCode` 不属于更新接口参数，无法通过该接口修改或补录空值，响应仍返回原有值。

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
