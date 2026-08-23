## 1. 建立接口与服务回归测试

- [x] 1.1 增加 Controller 契约测试，覆盖五个 `/api/information/*` POST 映射、上传必填参数及 `/api/images/*` 不再映射，并验证相关测试先失败后可用于回归。
- [x] 1.2 扩展 `ImageServiceImplTest` 的上传场景，覆盖 provinceCode/districtCode 非空校验、不执行六位数字校验、createTime 与 uploadTime 相等且符合 `yyyyMMddHHmmss`，并验证新增测试结果符合预期。
- [x] 1.3 扩展列表和更新单元测试，覆盖行政区精确 AND 查询、元数据更新与文件替换刷新 uploadTime、createTime 保持不变、行政区字段不可更新，并验证 MongoTemplate/GridFsTemplate 交互断言。

## 2. 修改数据模型与核心业务

- [x] 2.1 为 `ImageMetadata` 增加 provinceCode、districtCode、createTime 字段及手写 getter/setter，为 `ImageListRequest` 增加行政区筛选字段和 Swagger 描述，并通过编译与字段访问测试验证。
- [x] 2.2 修改上传 Service 签名与实现，校验两个行政区字段仅需非空、持久化其 trim 后的值，并以一次取时同时设置 createTime/uploadTime；运行上传相关单元测试验证。
- [x] 2.3 在列表查询中加入 provinceCode、districtCode 的非空精确条件并保留 uploadTime 降序排序；运行列表查询单元测试验证单项和组合条件。
- [x] 2.4 修改更新逻辑，仅在存在有效元数据字段或有效替换文件且更新成功时刷新 uploadTime，始终保留 createTime 与行政区字段；运行更新相关单元测试验证成功和文件失败场景。

## 3. 迁移公开 API 与联调契约

- [x] 3.1 将 Controller 基础路径替换为 `/api/information`，为 upload 增加必填 provinceCode/districtCode 参数并同步 Service 调用，保持 update 不暴露行政区参数；运行 Controller 契约测试验证新旧路径行为。
- [x] 3.2 更新 Swagger 注解、参数说明、示例和接口描述，运行 Swagger 文档测试并检查 `/v2/api-docs` 仅包含新路径及正确的必填字段。
- [x] 3.3 更新 `docs/api/frontend-integration.md` 中的全部地址、请求示例、筛选字段、响应元数据和时间格式说明，并人工核对五个接口与增量规范一致。

## 4. 提供历史数据迁移脚本

- [x] 4.1 在 `scripts/mongodb/` 编写默认 dry-run 的幂等 mongosh 脚本，覆盖旧/新时间识别、createTime 回填、行政区缺失字段回填、异常记录报告和汇总，并用代表性样例验证重复运行不会继续修改数据。
- [x] 4.2 编写迁移执行说明，包含目标数据库确认、备份、dry-run、正式执行、结果核对和回滚步骤，并核对所有命令均由用户手工触发且应用启动不会自动迁移。

## 5. 完整验证

- [x] 5.1 使用指定 Maven settings 运行 `mvnw.cmd -s "D:\develop\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\conf\settings.xml" test`，确认全部测试通过且无编译错误。
- [x] 5.2 运行 `openspec validate migrate-image-api-to-information --strict`，确认 proposal、delta spec、design 和 tasks 全部通过严格校验。
- [x] 5.3 在可用的 MySQL、MongoDB 环境启动服务，验证启动日志中的 Swagger 地址，并通过 Swagger 冒烟调用上传、列表、详情、更新、删除新接口，确认旧路径不可用及返回数据字段/时间格式正确。
