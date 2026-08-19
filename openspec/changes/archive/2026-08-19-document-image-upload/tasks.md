## 1. 脚手架：DTO、Entity、Service 接口

- [ ] 1.1 创建 `ApiResponse`（code/message/data 字段 + `success`/`error` 静态工厂）
- [ ] 1.2 创建请求 DTO：`ImageListRequest`、`ImageDetailRequest`、`ImageUpdateRequest`、`ImageDeleteRequest`
- [ ] 1.3 创建 `ImageMetadata` 实体（`@Document(collection="image_metadata")`，字段全 String，手动 getter/setter）
- [ ] 1.4 创建 `ImageService` 接口（upload/list/detail/update/delete 五个方法签名）
- [ ] 1.5 编译验证脚手架

## 2. 实现 upload — 上传图片及备注

- [ ] 2.1 编写 upload 单元测试（校验 GridFS store + MongoTemplate save）
- [ ] 2.2 实现 upload（校验 file/title 非空，GridFS 存文件，写入元数据）
- [ ] 2.3 运行测试验证通过

## 3. 实现 list — 分页查询列表

- [ ] 3.1 编写 list 单元测试（分页 + tag/uploader 筛选）
- [ ] 3.2 实现 list（tag 模糊匹配、uploader 精确匹配、uploadTime 降序、page/size 分页）
- [ ] 3.3 运行测试验证通过

## 4. 实现 detail — 查询详情 + 图片 base64

- [ ] 4.1 编写 detail 单元测试（返回元数据 + Base64；记录不存在）
- [ ] 4.2 实现 detail（findById + GridFS 读文件转 Base64）
- [ ] 4.3 运行测试验证通过

## 5. 实现 update — 更新备注信息

- [ ] 5.1 编写 update 单元测试（非空字段覆盖；记录不存在）
- [ ] 5.2 实现 update（非空字段才覆盖，可选替换图片）
- [ ] 5.3 运行测试验证通过

## 6. 实现 delete — 删除图片及备注

- [ ] 6.1 编写 delete 单元测试（删 GridFS + 元数据；记录不存在）
- [ ] 6.2 实现 delete（删 GridFS 文件 + 删元数据文档）
- [ ] 6.3 运行测试验证通过

## 7. 全局异常处理

- [ ] 7.1 创建 `GlobalExceptionHandler`（`@ControllerAdvice`，处理 `MaxUploadSizeExceededException` / `IllegalArgumentException` / 通用 `Exception`）
- [ ] 7.2 编译验证

## 8. REST 接口层

- [ ] 8.1 创建 `ImageController`（5 个 `@PostMapping` 端点，路径 `/api/images/*`）
- [ ] 8.2 编译验证

## 9. 配置与整体验证

- [ ] 9.1 在 `application.yaml` 配置文件上传大小限制（50MB）
- [ ] 9.2 全量编译 + 运行所有测试
