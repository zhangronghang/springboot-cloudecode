# 图片上传与备注管理接口 — 设计文档

> 日期: 2026-06-27 | 状态: 已确认

## 概述

提供一组 REST 接口，支持用户上传图片及备注信息。图片文件存入 MongoDB GridFS，备注信息存入 MongoDB `image_metadata` collection。

## 技术决策

| 项目 | 决策 |
|------|------|
| 图片存储 | MongoDB GridFS |
| 备注存储 | MongoDB `image_metadata` collection |
| 接口风格 | 全部 POST |
| 统一响应 | `{code, message, data}` |
| Java 版本 | 8 |
| 框架 | Spring Data MongoDB (MongoTemplate + GridFsTemplate) |

## 数据模型

Collection: `image_metadata`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | ObjectId | MongoDB 自动生成 |
| `title` | String | 图片标题 |
| `description` | String | 图片描述/备注 |
| `tags` | String | 标签（逗号分隔） |
| `uploader` | String | 上传者 |
| `uploadTime` | String | 上传时间（自动生成） |
| `fileSize` | String | 文件大小（字节，自动获取） |
| `fileName` | String | 原始文件名（自动获取） |
| `gridFsFileId` | String | GridFS 文件 ObjectId |

所有自定义字段均为 String 类型。

## API 接口

基础路径: `/api/images`

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

- `code`: 200 成功，400 参数错误，500 服务端错误
- `message`: 提示信息
- `data`: 具体数据

### POST `/api/images/upload`

上传图片及备注。

- Content-Type: `multipart/form-data`
- 参数:
  - `file`: 图片文件（必填）
  - `title`: 标题（必填）
  - `description`: 描述
  - `tags`: 标签
  - `uploader`: 上传者
- 响应 `data`: 上传成功的记录（含 `id`）

### POST `/api/images/list`

分页查询列表，支持筛选。

- Content-Type: `application/json`
- Body:
  - `page`: 页码（默认 1）
  - `size`: 每页条数（默认 10）
  - `tag`: 标签筛选（可选）
  - `uploader`: 上传者筛选（可选）
- 响应 `data`:
  - `total`: 总条数
  - `page`: 当前页
  - `size`: 每页条数
  - `records`: 记录列表

### POST `/api/images/detail`

查询单条详情。

- Body:
  - `id`: 记录 ID
- 响应 `data`: 完整备注信息 + 图片 base64

### POST `/api/images/update`

更新备注信息。

- Body:
  - `id`: 记录 ID（必填）
  - `title`、`description`、`tags`、`uploader`（至少填一项）

### POST `/api/images/delete`

删除图片及备注。

- Body:
  - `id`: 记录 ID

## 架构分层

```
Controller (ImageController)
    │ 接收请求，参数校验
    │
Service (ImageService / ImageServiceImpl)
    │ 业务逻辑
    │
    ├── GridFsTemplate   ← 存取/删除 GridFS 图片文件
    └── MongoTemplate    ← CRUD image_metadata 文档
```

## GridFS 操作流程

1. **上传**: `GridFsTemplate.store(inputStream, fileName, contentType)` → 获取 `gridFsFileId` → 写入 `image_metadata`
2. **读取**: `GridFsTemplate.findOne(query)` → `GridFsTemplate.getResource(gridFsFile)` → 读取内容
3. **删除**: `GridFsTemplate.delete(query)` 删除文件 → `MongoTemplate.remove()` 删除备注文档

## 错误处理

- `@ControllerAdvice` 全局异常处理器
- 参数校验：`title` 和 `file` 不能为空
- GridFS / MongoDB 操作异常返回 500 + 错误信息
- 记录不存在返回 400 + "记录不存在"

## 新增文件

```
src/main/java/com/example/springbootcoludecode/
├── controller/
│   └── ImageController.java
├── service/
│   ├── ImageService.java
│   └── impl/
│       └── ImageServiceImpl.java
├── entity/
│   └── ImageMetadata.java
├── dto/
│   ├── ImageListRequest.java
│   ├── ImageDetailRequest.java
│   ├── ImageUpdateRequest.java
│   ├── ImageDeleteRequest.java
│   └── ApiResponse.java
└── config/
    └── GlobalExceptionHandler.java
```