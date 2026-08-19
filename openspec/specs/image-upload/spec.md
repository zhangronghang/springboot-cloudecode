## Purpose

图片上传与备注管理能力：支持用户上传图片文件及备注信息，并提供列表查询、详情查看、更新与删除的完整接口。

## Requirements

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
系统 SHALL 支持通过 multipart/form-data 上传图片文件及备注信息，将图片文件持久化存储，并将备注写入元数据记录。

#### Scenario: 上传成功
- **WHEN** 提交有效的图片文件与标题（file 与 title 均非空）
- **THEN** 图片文件被持久化存储，备注写入元数据记录，返回 code=200 及完整记录（含 id）

#### Scenario: 缺少图片文件
- **WHEN** file 为空
- **THEN** 返回 code=400，提示图片文件不能为空

#### Scenario: 缺少标题
- **WHEN** title 为空
- **THEN** 返回 code=400，提示标题不能为空

### Requirement: 列表分页查询
系统 SHALL 支持分页查询元数据列表，支持按标签模糊匹配与按上传者精确匹配过滤，并按上传时间降序排列。

#### Scenario: 分页查询
- **WHEN** 提交 page 与 size（默认分别为 1 与 10）
- **THEN** 返回 total、page、size 与 records 列表

#### Scenario: 按标签与上传者过滤
- **WHEN** 提供 tag 或 uploader 筛选条件
- **THEN** 仅返回标签模糊匹配、上传者精确匹配的记录

### Requirement: 详情查询
系统 SHALL 支持根据记录 id 查询单条记录，返回完整备注信息及图片的 Base64 编码内容。

#### Scenario: 查询存在的记录
- **WHEN** 提供有效的记录 id
- **THEN** 返回完整元数据与图片 Base64 内容

#### Scenario: 记录不存在
- **WHEN** 提供的 id 对应记录不存在
- **THEN** 返回 code=400，提示记录不存在

### Requirement: 更新备注
系统 SHALL 支持根据 id 更新记录的元数据字段（仅非空字段被覆盖），并可选择性地替换图片文件。

#### Scenario: 更新元数据字段
- **WHEN** 提供 id 及需更新的字段（title、description、tags、uploader）
- **THEN** 仅覆盖非空字段，未提供的字段保持不变，返回更新后的记录

#### Scenario: 替换图片文件
- **WHEN** 提供新的图片文件
- **THEN** 新文件持久化成功后再删除旧文件，并更新 gridFsFileId、fileName、fileSize

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
图片元数据记录 SHALL 包含以下字段：id（自动生成）、title（标题）、description（描述）、tags（标签，逗号分隔）、uploader（上传者）、uploadTime（上传时间，自动生成）、fileSize（文件大小字节数，自动获取）、fileName（原始文件名，自动获取）、gridFsFileId（图片文件标识）。所有自定义字段均为字符串类型。

#### Scenario: 记录字段完整
- **WHEN** 记录被创建
- **THEN** 记录包含上述全部字段，其中自动字段由系统填充
