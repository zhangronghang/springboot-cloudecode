## MODIFIED Requirements

### Requirement: 图片上传
系统 SHALL 支持通过 multipart/form-data 上传图片文件及备注信息，其中 file 与 title 为必填非空字段，provinceCode、cityCode 与 districtCode 均为可选字段。系统 SHALL 独立规范化三个行政区划代码：字段缺失、为空或仅包含空白字符时保存为 `""`，非空时去除首尾空白后保存；系统不得校验其字符组成、长度、上下级关系或字段组合。系统 SHALL 持久化图片文件和元数据，并在首次上传时使用同一个当前时间值填写 createTime 与 uploadTime。

#### Scenario: 上传成功
- **WHEN** 提交有效的图片文件、非空的 title，并以任意组合提供 provinceCode、cityCode 和 districtCode
- **THEN** 图片文件被持久化，元数据包含独立规范化后的三个行政区划代码，返回 code=200 及完整记录（含 id）
- **AND** createTime 与 uploadTime 完全相同且均符合 `yyyyMMddHHmmss`

#### Scenario: 三个行政区字段均未提供
- **WHEN** 提交有效的 file 与 title，但 provinceCode、cityCode 和 districtCode 均缺失
- **THEN** 上传成功，三个行政区划字段均持久化为 `""`

#### Scenario: 缺少省级行政区划代码
- **WHEN** 提交有效的 file、title、cityCode 和 districtCode，但 provinceCode 缺失、为空或仅包含空白字符
- **THEN** 上传成功且持久化的 provinceCode 为 `""`

#### Scenario: 缺少市级行政区划代码
- **WHEN** 提交有效的 file 与 title，但 cityCode 缺失、为空或仅包含空白字符
- **THEN** 上传成功且持久化的 cityCode 为 `""`

#### Scenario: 缺少区县级行政区划代码
- **WHEN** 提交有效的 file 与 title，但 districtCode 缺失、为空或仅包含空白字符
- **THEN** 上传成功且持久化的 districtCode 为 `""`

#### Scenario: 行政区字段为空或仅包含空白
- **WHEN** provinceCode、cityCode 或 districtCode 为空或仅包含空白字符
- **THEN** 上传成功，对应字段持久化为 `""`

#### Scenario: 接受行政区字段的任意组合
- **WHEN** 仅提供 provinceCode、cityCode 或 districtCode 中的部分字段
- **THEN** 系统独立保存每个已提供字段的规范化值，并将其他字段保存为 `""`

#### Scenario: 接受任意非空行政区代码
- **WHEN** 任一行政区划代码非空但不符合六位数字格式或行政区上下级关系
- **THEN** 系统不因字符组成、长度、层级关系或字段组合拒绝本次上传

#### Scenario: 缺少图片文件
- **WHEN** file 为空
- **THEN** 返回 code=400，提示图片文件不能为空

#### Scenario: 缺少标题
- **WHEN** title 为空
- **THEN** 返回 code=400，提示标题不能为空

### Requirement: 元数据字段
图片元数据记录 SHALL 包含以下字符串字段：id（自动生成）、title（标题）、description（描述）、tags（标签，逗号分隔）、uploader（上传者）、provinceCode（省级行政区划代码）、cityCode（市级行政区划代码）、districtCode（区县级行政区划代码）、createTime（创建时间）、uploadTime（最近一次上传或更新的时间）、fileSize（文件大小字节数，自动获取）、fileName（原始文件名，自动获取）、gridFsFileId（图片文件标识）。createTime 与 uploadTime SHALL 使用 `yyyyMMddHHmmss` 格式。新建记录的三个行政区划字段 SHALL 均为非 null 字符串，缺失或空白的上传值 SHALL 记录为 `""`。

#### Scenario: 记录字段完整
- **WHEN** 新记录被创建
- **THEN** 记录包含上述全部字段，其中自动字段由系统填充，三个行政区划字段使用规范化后的上传请求值且均不为 null

#### Scenario: 创建时间不可变
- **WHEN** 已有记录的元数据或图片文件被更新
- **THEN** createTime 保持首次创建时的值不变

#### Scenario: 行政区字段创建后不可修改
- **WHEN** 已有记录的元数据或图片文件被更新
- **THEN** provinceCode、cityCode 与 districtCode 均保持首次创建时的值不变

#### Scenario: 兼容行政区字段缺失的旧记录
- **WHEN** 读取、查询、更新或删除历史记录且该记录不存在 provinceCode、cityCode 或 districtCode 中的任意字段
- **THEN** 系统继续正常处理该记录，不因行政区字段缺失而报错，也不自动补写该字段

#### Scenario: 兼容缺少市级代码的旧记录
- **WHEN** 读取、查询、更新或删除历史记录且该记录不存在 cityCode 字段
- **THEN** 系统继续正常处理该记录，不因 cityCode 缺失而报错，也不自动补写该字段
