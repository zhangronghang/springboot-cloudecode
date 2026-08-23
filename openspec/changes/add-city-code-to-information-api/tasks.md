## 1. 先建立失败的接口与业务测试

- [ ] 1.1 扩展 Controller 契约测试，覆盖上传缺少 cityCode 返回 400、provinceCode 缺失或空白仍可进入 Service、districtCode 继续必填，并先运行目标测试确认新增断言在实现前失败。
- [ ] 1.2 扩展 ImageServiceImplTest 的上传场景，覆盖 cityCode/districtCode 非空校验、provinceCode 空白归一化为 `""`、三个代码 trim 后持久化及不执行格式/层级校验，并先运行目标测试确认失败原因与缺失行为一致。
- [ ] 1.3 扩展列表、更新和旧数据兼容测试，覆盖 cityCode 精确 AND 查询、空白 cityCode 不生成条件、三个行政区字段更新后保持原值，以及 cityCode 为 null 的记录仍可详情/更新/删除，并先运行目标测试确认新增行为尚未实现。

## 2. 扩展数据模型与核心业务

- [ ] 2.1 为 ImageMetadata 和 ImageListRequest 增加 cityCode 及手写 getter/setter，修改上传 Service 签名以接收 cityCode，并通过编译和对应字段契约测试验证。
- [ ] 2.2 修改上传实现：provinceCode 缺失或空白时保存 `""`，cityCode 与 districtCode 必须非空，三个代码 trim 后持久化且不校验格式/层级；运行上传单元测试验证全部场景通过。
- [ ] 2.3 在列表查询中为非空 cityCode 增加精确 Criteria，并与现有筛选条件组成 AND；运行列表查询单元测试验证单项、组合和空白忽略行为。
- [ ] 2.4 保持更新 Service 不接收行政区参数、详情和删除仅按 ID 工作，并运行更新/详情/删除兼容测试确认 cityCode 缺失不会报错且已有行政区值不变。

## 3. 同步公开接口、Swagger 与联调文档

- [ ] 3.1 修改上传 Controller：provinceCode 调整为可选、cityCode 新增为必填并传入 Service，update/detail/delete 不新增行政区请求参数；运行 Controller 契约测试验证参数与路由行为。
- [ ] 3.2 更新 Swagger 注解和文档测试，标明 provinceCode 可选、cityCode/districtCode 必填及列表 cityCode 精确筛选，并检查 `/v2/api-docs` 中参数 required 状态和说明正确。
- [ ] 3.3 更新 `docs/api/frontend-integration.md` 的字段说明、普通地区/空 provinceCode 上传示例、列表查询示例、不可更新规则和旧记录 cityCode 兼容说明，并人工核对五个接口与增量规范一致。

## 4. 完整验证

- [ ] 4.1 使用 `mvnw.cmd -s "D:\develop\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\conf\settings.xml" test` 运行完整测试，确认全部测试通过且无编译错误。
- [ ] 4.2 运行 `openspec validate add-city-code-to-information-api --strict`，确认 proposal、delta spec、design 和 tasks 全部通过严格校验。
- [ ] 4.3 在可用的 MySQL、MongoDB 环境启动服务，通过 Swagger 冒烟验证普通上传、空 provinceCode 上传、缺失 cityCode 错误、cityCode 查询及原有详情/更新/删除，并确认没有执行任何历史数据迁移。
