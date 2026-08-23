## 1. 测试先行（Red）

- [x] 1.1 修改 `ImageServiceImplTest`，将 cityCode、districtCode 空值拒绝用例改为覆盖三个行政区字段全部缺失、空白、部分提供和非标准值均成功且按规则规范化；使用 IntelliJ 内置 `mvn.cmd -s 'D:\develop\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\conf\settings.xml' -Dtest=ImageServiceImplTest test` 验证测试在旧实现上因 cityCode/districtCode 仍返回 400 而失败
- [x] 1.2 修改 `SwaggerDocumentationTest`，断言上传接口的 provinceCode、cityCode、districtCode 均为非必填，且接口说明只保留 file、title 的必填错误；使用 IntelliJ 内置 `mvn.cmd -s 'D:\develop\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\conf\settings.xml' -Dtest=SwaggerDocumentationTest test` 验证测试在旧 Swagger 注解上失败

## 2. 上传行为实现（Green）

- [x] 2.1 删除 `ImageServiceImpl.upload` 对 cityCode、districtCode 的非空拒绝逻辑，并对 provinceCode、cityCode、districtCode 统一执行 null/空白转 `""`、非空值去首尾空白的规范化；重新运行 `ImageServiceImplTest` 并验证全部通过
- [x] 2.2 更新 `ImageController.upload` 的 Swagger 操作说明、参数必填标记、参数描述与 400 响应说明；重新运行 `SwaggerDocumentationTest` 并验证全部通过
- [x] 2.3 运行现有列表与更新相关测试，验证空白行政区筛选仍被忽略、非空值仍精确匹配、更新接口仍不声明或修改三个行政区字段

## 3. 联调文档

- [x] 3.1 更新 `docs/api/frontend-integration.md`，将三个行政区字段统一列为可选，说明缺失/空白保存 `""`、任意组合可用、不做格式和层级校验，并提供全部省略及部分提供的上传示例；人工核对文档不再把 cityCode 或 districtCode 描述为必填
- [x] 3.2 更新联调文档中的成功响应、400 错误和兼容性说明，明确新记录三个行政区字段非 null、列表不支持专门查询空行政区、更新接口不能补录行政区字段；对照增量规范逐项核对内容一致

## 4. 完整验证

- [x] 4.1 使用 IntelliJ 内置 `mvn.cmd -s 'D:\develop\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\conf\settings.xml' test` 运行完整测试套件并验证构建成功且无测试失败
- [x] 4.2 使用 `openspec validate make-upload-region-codes-optional --strict` 严格校验变更，并检查 Git 差异确认未修改数据库结构、列表/更新/详情/删除实现或历史迁移脚本；主规范通过后续 `openspec-sync-specs` 工作流同步
