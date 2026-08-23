## Purpose

图片上传与备注管理能力：支持用户上传图片文件及备注信息，并提供列表查询、详情查看、更新与删除的完整接口。

## Requirements

### Requirement: 信息管理接口路径
系统 SHALL 仅通过 `POST /api/information/upload`、`POST /api/information/list`、`POST /api/information/detail`、`POST /api/information/update` 和 `POST /api/information/delete` 提供图片信息管理能力，不再提供对应的 `/api/images/*` 路径。

#### Scenario: 使用新路径访问接口
- **WHEN** 客户端使用 `/api/information` 前缀调用 upload、list、detail、update 或 delete
- **THEN** 系统将请求路由到对应的图片信息管理接口

#### Scenario: 访问已删除的旧路径
- **WHEN** 客户端调用任意原 `/api/images/*` 接口
- **THEN** 系统不存在该旧路径的可用接口映射

### Requirement: 统一响应格式
所有接口 SHALL 返回统一结构 `{code, message, data}`，其中 `code` 表示结果状态（200 成功、400 参数错误、500 服务器错误），`message` 为提示信息，`data` 为业务数据。

#### Scenario: 成功响应
- **WHEN** 接口正常处理请求
- **THEN** 返回 code=200、message="success"，data 为业务数据

#### Scenario: 参数错误响应
- **WHEN** 请求参数缺失或不合法
- **THEN** 返回 code=400 及对应错误信息

#### Scenario: 服务端错误响应
- **WHEN** 发生未预期异常
- **THEN** 返回 code=500 及错误信息

### Requirement: 图片上传
系统 SHALL 支持通过 multipart/form-data 上传图片文件及备注信息，其中 file、title、provinceCode 和 districtCode 均为必填非空字段；provinceCode 与 districtCode 仅校验非空，不校验字符组成或长度。系统 SHALL 持久化图片文件和元数据，并在首次上传时使用同一个当前时间值填写 createTime 与 uploadTime。

#### Scenario: 上传成功
- **WHEN** 提交有效的图片文件以及非空的 title、provinceCode 和 districtCode
- **THEN** 图片文件被持久化，元数据包含提交的行政区字段，返回 code=200 及完整记录（含 id）
- **AND** createTime 与 uploadTime 完全相同且均符合 `yyyyMMddHHmmss`

#### Scenario: 接受任意非空行政区代码
- **WHEN** provinceCode 与 districtCode 非空但不符合六位数字格式
- **THEN** 系统不因字符组成或长度拒绝本次上传

#### Scenario: 缺少图片文件
- **WHEN** file 为空
- **THEN** 返回 code=400，提示图片文件不能为空

#### Scenario: 缺少标题
- **WHEN** title 为空
- **THEN** 返回 code=400，提示标题不能为空

#### Scenario: 缺少省级行政区划代码
- **WHEN** provinceCode 缺失、为空或仅包含空白字符
- **THEN** 返回 code=400，提示省级行政区划代码不能为空

#### Scenario: 缺少区县级行政区划代码
- **WHEN** districtCode 缺失、为空或仅包含空白字符
- **THEN** 返回 code=400，提示区县级行政区划代码不能为空

### Requirement: 列表分页查询
系统 SHALL 支持分页查询元数据列表，支持按标签模糊匹配，以及按上传者、provinceCode、districtCode 精确匹配过滤；多个筛选条件同时提供时 SHALL 同时满足所有条件，并按 uploadTime 降序排列。

#### Scenario: 分页查询
- **WHEN** 提交 page 与 size（默认分别为 1 与 10）
- **THEN** 返回 total、page、size 与 records 列表

#### Scenario: 按标签与上传者过滤
- **WHEN** 提供 tag 或 uploader 筛选条件
- **THEN** 仅返回标签模糊匹配、上传者精确匹配的记录

#### Scenario: 按行政区过滤
- **WHEN** 提供 provinceCode 或 districtCode 筛选条件
- **THEN** 仅返回对应字段精确匹配的记录

#### Scenario: 组合筛选
- **WHEN** 同时提供 tag、uploader、provinceCode 和 districtCode 中的多个条件
- **THEN** 仅返回同时满足全部已提供条件的记录

### Requirement: 详情查询
系统 SHALL 支持根据记录 id 查询单条记录，返回完整备注信息及图片的 Base64 编码内容。

#### Scenario: 查询存在的记录
- **WHEN** 提供有效的记录 id
- **THEN** 返回完整元数据与图片 Base64 内容

#### Scenario: 记录不存在
- **WHEN** 提供的 id 对应记录不存在
- **THEN** 返回 code=400，提示记录不存在

### Requirement: 更新备注
系统 SHALL 支持根据 id 更新记录的 title、description、tags、uploader 等元数据字段（仅非空字段被覆盖），并可选择性地替换图片文件。provinceCode 与 districtCode 不属于可更新字段。任意元数据字段实际更新或图片文件替换成功时，系统 SHALL 将 uploadTime 设置为符合 `yyyyMMddHHmmss` 的当前时间，并保持 createTime 不变。

#### Scenario: 更新元数据字段
- **WHEN** 提供 id 及至少一个非空的可更新字段（title、description、tags、uploader）
- **THEN** 仅覆盖非空字段，未提供的字段保持不变，uploadTime 更新为当前时间，返回更新后的记录

#### Scenario: 替换图片文件
- **WHEN** 提供新的图片文件
- **THEN** 新文件持久化成功后再删除旧文件，并更新 gridFsFileId、fileName、fileSize 和 uploadTime
- **AND** createTime 保持不变

#### Scenario: 尝试修改行政区字段
- **WHEN** 更新请求中携带 provinceCode 或 districtCode
- **THEN** 已存储的 provinceCode 与 districtCode 保持不变

#### Scenario: 记录不存在
- **WHEN** 提供的 id 对应记录不存在
- **THEN** 返回 code=400，提示记录不存在

### Requirement: 删除记录
系统 SHALL 支持根据 id 删除记录，同时删除其关联的图片文件与元数据。

#### Scenario: 删除成功
- **WHEN** 提供有效的记录 id
- **THEN** 删除关联的图片文件与元数据记录，返回 code=200

#### Scenario: 记录不存在
- **WHEN** 提供的 id 对应记录不存在
- **THEN** 返回 code=400，提示记录不存在

### Requirement: 元数据字段
图片元数据记录 SHALL 包含以下字符串字段：id（自动生成）、title（标题）、description（描述）、tags（标签，逗号分隔）、uploader（上传者）、provinceCode（省级行政区划代码）、districtCode（区县级行政区划代码）、createTime（创建时间）、uploadTime（最近一次上传或更新的时间）、fileSize（文件大小字节数，自动获取）、fileName（原始文件名，自动获取）、gridFsFileId（图片文件标识）。createTime 与 uploadTime SHALL 使用 `yyyyMMddHHmmss` 格式。

#### Scenario: 记录字段完整
- **WHEN** 记录被创建
- **THEN** 记录包含上述全部字段，其中自动字段由系统填充，provinceCode 与 districtCode 使用上传请求值

#### Scenario: 创建时间不可变
- **WHEN** 已有记录的元数据或图片文件被更新
- **THEN** createTime 保持首次创建时的值不变

### Requirement: 历史元数据迁移
系统 SHALL 提供由用户手工执行的幂等 MongoDB 迁移脚本，将 `image_metadata` 中格式为 `yyyy-MM-dd HH:mm:ss` 的历史 `uploadTime` 转换为 `yyyyMMddHHmmss`，使用该历史时间回填缺失的 `createTime`，并将缺失的 `provinceCode` 和 `districtCode` 回填为空字符串。脚本 MUST 报告无法识别的历史时间记录，不得静默改写其时间数据。

#### Scenario: 迁移有效的历史记录
- **WHEN** 历史记录的 uploadTime 为可识别的 `yyyy-MM-dd HH:mm:ss` 格式，且新增字段尚不存在
- **THEN** uploadTime 被转换为 `yyyyMMddHHmmss`，createTime 被设置为相同值，provinceCode 与 districtCode 被设置为空字符串

#### Scenario: 迁移已使用新时间格式的历史记录
- **WHEN** 历史记录的 uploadTime 已符合 `yyyyMMddHHmmss`，但 createTime 或行政区字段缺失
- **THEN** 脚本保留 uploadTime，并以该值回填缺失的 createTime，以空字符串回填缺失的行政区字段

#### Scenario: 重复执行迁移脚本
- **WHEN** 用户对已完成迁移的记录再次执行脚本
- **THEN** 记录中的既有新格式时间和已存在字段保持不变

#### Scenario: 遇到无法识别的历史时间
- **WHEN** 某条记录的 uploadTime 既不符合旧格式也不符合新格式
- **THEN** 脚本报告该记录 id 与异常值，并跳过对该记录的改写
