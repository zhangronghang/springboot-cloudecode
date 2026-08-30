## MODIFIED Requirements

### Requirement: 信息管理接口路径
系统 SHALL 通过 `POST /api/information/upload`、`POST /api/information/list`、`POST /api/information/detail`、`POST /api/information/update`、`POST /api/information/delete`、`POST /api/information/image/add`、`POST /api/information/image/delete` 和 `POST /api/information/image/list` 提供足迹及其图片管理能力，并通过 `GET /api/information/{informationId}/images/{imageId}/thumbnail` 和 `GET /api/information/{informationId}/images/{imageId}/original` 提供图片二进制访问；系统 MUST NOT 恢复任何 `/api/images/*` 旧路径。

#### Scenario: 使用新路径访问接口
- **WHEN** 客户端通过 `/api/information` 下的既定 POST 路径创建、查询、修改或删除足迹及其图片
- **THEN** 系统将请求路由到对应的足迹或图片管理能力

#### Scenario: 通过永久地址访问图片
- **WHEN** 客户端调用某足迹下某图片的 thumbnail 或 original GET 地址
- **THEN** 系统返回对应缩略图或原图二进制

#### Scenario: 访问已删除的旧路径
- **WHEN** 客户端调用任意 `/api/images/*` 接口
- **THEN** 系统不存在该旧路径的可用接口映射

### Requirement: 统一响应格式
所有 POST 业务接口 SHALL 返回统一结构 `{code, message, data}`，其中 `code` 表示结果状态（200 成功、400 参数或业务错误、500 服务器错误），`message` 为提示信息，`data` 为业务数据。图片 thumbnail 和 original GET 接口 SHALL 直接返回二进制并使用真实 HTTP 状态码，不得包装为 `ApiResponse`。

#### Scenario: 成功响应
- **WHEN** POST 业务接口正常处理请求且没有专门的成功提示
- **THEN** 返回 code=200、message="success" 和对应业务数据

#### Scenario: 参数错误响应
- **WHEN** POST 业务接口收到缺失或不合法的参数
- **THEN** 返回 code=400 及对应错误信息

#### Scenario: 服务端错误响应
- **WHEN** POST 业务接口发生未预期异常
- **THEN** 返回 code=500 及对应错误信息

#### Scenario: 图片二进制访问失败
- **WHEN** thumbnail 或 original GET 请求中的参数非法、资源不存在或服务端读取失败
- **THEN** 系统分别返回真实 HTTP 400、404 或 500，且响应不使用统一 JSON 包装

### Requirement: 图片上传
系统 SHALL 支持通过 multipart/form-data 创建足迹，其中 title 与且仅一个 file 为必填非空字段，file MUST 是可实际解码的 JPEG 或 PNG；provinceCode、cityCode 与 districtCode 均为可选字段。系统 SHALL 独立规范化三个行政区划代码：字段缺失、为空或仅包含空白字符时保存为 `""`，非空时去除首尾空白后保存；系统不得校验其字符组成、长度、上下级关系或字段组合。系统 SHALL 为初始图片生成原图和缩略图，作为该足迹图片集合的第一项，并在首次创建时使用同一个当前时间值填写 createTime 与 uploadTime。

#### Scenario: 上传成功
- **WHEN** 提交一个有效 JPEG 或 PNG、非空 title，并以任意组合提供 provinceCode、cityCode 和 districtCode
- **THEN** 系统持久化足迹、原图和缩略图，图片成为该足迹的第一张，返回 code=200 及含 id、imageCount=1 和封面信息的公开记录
- **AND** createTime 与 uploadTime 完全相同且均符合 `yyyyMMddHHmmss`

#### Scenario: 三个行政区字段均未提供
- **WHEN** 提交有效 file 与 title，但 provinceCode、cityCode 和 districtCode 均缺失
- **THEN** 上传成功，三个行政区划字段均持久化为 `""`

#### Scenario: 缺少省级行政区划代码
- **WHEN** 提交有效 file 与 title，但 provinceCode 缺失、为空或仅包含空白字符
- **THEN** 上传成功且 provinceCode 持久化为 `""`

#### Scenario: 缺少市级行政区划代码
- **WHEN** 提交有效 file 与 title，但 cityCode 缺失、为空或仅包含空白字符
- **THEN** 上传成功且 cityCode 持久化为 `""`

#### Scenario: 缺少区县级行政区划代码
- **WHEN** 提交有效 file 与 title，但 districtCode 缺失、为空或仅包含空白字符
- **THEN** 上传成功且 districtCode 持久化为 `""`

#### Scenario: 行政区字段为空或仅包含空白
- **WHEN** provinceCode、cityCode 或 districtCode 中的任意字段为空或仅包含空白字符
- **THEN** 上传成功，对应字段持久化为 `""`

#### Scenario: 接受行政区字段的任意组合
- **WHEN** 仅提供 provinceCode、cityCode 或 districtCode 中的部分字段
- **THEN** 系统独立保存已提供字段的规范化值，并将其他字段保存为 `""`

#### Scenario: 接受任意非空行政区代码
- **WHEN** 任一行政区划代码非空但不符合六位数字格式、上下级关系或完整字段组合
- **THEN** 系统不因字符组成、长度、层级关系或字段组合拒绝创建，并保存去除首尾空白后的值

#### Scenario: 缺少图片文件
- **WHEN** 创建请求没有提供非空 file
- **THEN** 返回 code=400，提示图片文件不能为空

#### Scenario: 一次提交多张图片
- **WHEN** 创建请求包含两个或更多 file part
- **THEN** 返回 code=400，且不得只保存其中一张或产生任何足迹、原图、缩略图残留

#### Scenario: 缺少标题
- **WHEN** title 缺失、为空或仅包含空白
- **THEN** 返回 code=400，提示标题不能为空，且不产生任何足迹、原图或缩略图残留

#### Scenario: 上传不支持或损坏的文件
- **WHEN** file 不是可实际解码的 JPEG 或 PNG，或者其声明类型与实际内容不一致
- **THEN** 返回 code=400，且不产生任何足迹、原图或缩略图残留

### Requirement: 列表分页查询
系统 SHALL 支持分页查询足迹元数据列表，支持按标签模糊匹配，以及按上传者、provinceCode、cityCode、districtCode 精确匹配过滤；多个筛选条件同时提供时 SHALL 同时满足所有条件，空白筛选值 SHALL 被忽略，并按 uploadTime 降序排列。每条记录 SHALL 返回 imageCount；有图片时 coverImage SHALL 只描述图片集合第一项并包含 imageId、thumbnailUrl 和 originalUrl，无图片时 coverImage SHALL 为 null。列表 MUST NOT 返回图片 Base64 或其他图片二进制。

#### Scenario: 分页查询
- **WHEN** 提交 page 与 size（默认分别为 1 与 10）
- **THEN** 返回 total、page、size 与 records 列表，每条记录包含 imageCount 和 coverImage

#### Scenario: 按标签与上传者过滤
- **WHEN** 提供 tag 或 uploader 筛选条件
- **THEN** 仅返回标签模糊匹配、上传者精确匹配的足迹

#### Scenario: 按行政区过滤
- **WHEN** 提供 provinceCode、cityCode 或 districtCode 中的任意非空筛选条件
- **THEN** 仅返回对应字段精确匹配的足迹

#### Scenario: 组合筛选
- **WHEN** 同时提供 tag、uploader、provinceCode、cityCode 和 districtCode 中的多个非空条件
- **THEN** 仅返回同时满足全部已提供条件的足迹

#### Scenario: 忽略空白行政区筛选
- **WHEN** provinceCode、cityCode 或 districtCode 的查询值缺失、为空或仅包含空白字符
- **THEN** 系统不为对应字段添加查询条件

#### Scenario: 足迹有图片
- **WHEN** 返回的足迹图片集合不为空
- **THEN** coverImage 仅引用第一张图片，imageCount 等于图片集合数量，响应不包含 Base64

#### Scenario: 足迹没有图片
- **WHEN** 返回的足迹图片集合为空
- **THEN** coverImage 为 null 且 imageCount=0

### Requirement: 详情查询
系统 SHALL 支持根据足迹 id 查询单条足迹，只返回公开元数据和 imageCount，不返回内部图片数组、GridFS 文件标识或图片 Base64，且该查询不得依赖读取任何图片二进制。

#### Scenario: 查询存在的记录
- **WHEN** 提供有效的足迹 id
- **THEN** 返回完整公开元数据与 imageCount，不包含 imageBase64、内部图片数组或 GridFS 文件标识

#### Scenario: 记录不存在
- **WHEN** 提供的 id 对应足迹不存在
- **THEN** 返回 code=400，提示记录不存在

### Requirement: 更新备注
系统 SHALL 支持根据 id 更新足迹的 title、description、tags、uploader 等元数据字段，且仅非空字段被覆盖。update 接口 MUST NOT 接收或替换图片，provinceCode、cityCode 与 districtCode 仍不属于可更新字段。任意元数据字段实际更新时，系统 SHALL 将 uploadTime 设置为符合 `yyyyMMddHHmmss` 的当前时间，并保持 createTime 不变。

#### Scenario: 更新元数据字段
- **WHEN** 提供 id 及至少一个非空的可更新字段（title、description、tags、uploader）
- **THEN** 仅覆盖非空字段，未提供的字段和图片集合保持不变，uploadTime 更新为当前时间，返回更新后的公开记录

#### Scenario: 替换图片文件
- **WHEN** 客户端向 update 请求携带 file
- **THEN** 该参数不属于接口契约，系统不得借此新增、替换或删除任何图片

#### Scenario: 尝试修改行政区字段
- **WHEN** 更新请求中携带 provinceCode、cityCode 或 districtCode
- **THEN** 已存储的三个行政区划字段保持不变

#### Scenario: 记录不存在
- **WHEN** 提供的 id 对应足迹不存在
- **THEN** 返回 code=400，提示记录不存在

### Requirement: 删除记录
系统 SHALL 支持根据 id 删除整条足迹，并清理该足迹图片集合引用的全部原图和缩略图。图片文件已经缺失时不得阻止足迹元数据删除；文件清理失败时系统 MUST 报告可供后续清理的文件标识，但不得恢复已经删除的足迹元数据。

#### Scenario: 删除成功
- **WHEN** 提供有效的足迹 id
- **THEN** 删除足迹元数据并尝试清理其全部原图和缩略图，返回 code=200

#### Scenario: 删除没有图片的足迹
- **WHEN** 提供的足迹存在但图片集合为空
- **THEN** 删除足迹元数据并返回 code=200

#### Scenario: 记录不存在
- **WHEN** 提供的 id 对应足迹不存在
- **THEN** 返回 code=400，提示记录不存在

### Requirement: 元数据字段
足迹元数据记录 SHALL 包含字符串字段 id、title、description、tags、uploader、provinceCode、cityCode、districtCode、createTime、uploadTime，以及允许为空且最多包含 50 项的有序 images 数组。每个图片项 SHALL 包含稳定业务 imageId、内部原图标识、内部缩略图标识、fileName、以数字字节数表示的 fileSize、contentType、原图 width、原图 height 和 createTime。createTime 与 uploadTime SHALL 使用 `yyyyMMddHHmmss` 格式；imageCount SHALL 根据 images 数组长度计算而不得作为独立持久化字段。

#### Scenario: 记录字段完整
- **WHEN** 新足迹被创建
- **THEN** 足迹包含全部规定字段，images 仅包含初始图片，三个行政区划字段均为非 null 字符串

#### Scenario: 图片顺序决定封面
- **WHEN** 足迹 images 数组非空
- **THEN** 数组第一项是列表封面，后续新增图片追加到数组末尾

#### Scenario: 删除当前封面
- **WHEN** images 第一项被删除且数组仍有其他图片
- **THEN** 删除后的第一项自动成为新封面

#### Scenario: 图片数组为空
- **WHEN** 足迹最后一张图片被删除
- **THEN** images 保存为空数组且足迹仍然存在

#### Scenario: 创建时间不可变
- **WHEN** 已有足迹的元数据或图片集合发生变化
- **THEN** 足迹 createTime 保持首次创建值不变，实际变化时 uploadTime 更新为当前时间

#### Scenario: 行政区字段创建后不可修改
- **WHEN** 已有足迹的元数据或图片集合发生变化
- **THEN** provinceCode、cityCode 与 districtCode 均保持首次创建时的值不变

#### Scenario: 兼容行政区字段缺失的旧记录
- **WHEN** 发布前历史记录缺少 provinceCode、cityCode 或 districtCode 中的任意字段
- **THEN** 一次性历史清理删除该足迹及其关联图片，新版本不读取、补写或迁移该旧记录

#### Scenario: 兼容缺少市级代码的旧记录
- **WHEN** 发布前历史记录不存在 cityCode 字段
- **THEN** 一次性历史清理删除该足迹及其关联图片，新版本不读取、补写或迁移该旧记录

## ADDED Requirements

### Requirement: 独立新增足迹图片
系统 SHALL 通过 `POST /api/information/image/add` 接收 informationId 和且仅一个非空 file，为存在且图片数量少于 50 的足迹新增一张图片。新增图片 SHALL 使用稳定业务 imageId，生成原图与缩略图，追加到图片集合末尾，并返回不包含内部文件标识的公开图片数据及永久相对访问地址。

#### Scenario: 新增一张图片
- **WHEN** 为图片数量少于 50 的现有足迹提交一个有效 JPEG 或 PNG
- **THEN** 新图片被追加到集合末尾，uploadTime 更新，返回 code=200、公开图片数据、thumbnailUrl 和 originalUrl

#### Scenario: 向无图片足迹新增图片
- **WHEN** 为 images 为空的足迹新增一个有效文件
- **THEN** 新图片成为图片集合第一项和列表封面

#### Scenario: 一次提交多张新增图片
- **WHEN** 新增请求包含两个或更多 file part
- **THEN** 返回 code=400，且不得新增任何图片或残留任何本次文件

#### Scenario: 达到图片数量上限
- **WHEN** 足迹已有 50 张图片
- **THEN** 新增请求返回 code=400，且不得持久化本次原图、缩略图或图片元数据

#### Scenario: 并发尝试突破上限
- **WHEN** 多个并发新增请求可能使同一足迹超过 50 张图片
- **THEN** 系统仅接受不会使最终数量超过 50 的请求，其他请求返回 code=400 且不残留文件

#### Scenario: 新增图片的足迹不存在
- **WHEN** informationId 对应足迹不存在
- **THEN** 返回 code=400，且不产生图片文件或元数据残留

### Requirement: 快速删除足迹图片
系统 SHALL 通过 `POST /api/information/image/delete` 接收 informationId 和非空 imageIds 数组，一次删除一张或多张图片。系统 SHALL 对重复 imageId 去重，忽略无效、已删除或不属于该足迹的 imageId，并继续删除其他有效图片。系统 SHALL 允许删除最后一张图片，并返回去重后的 requestedCount、实际 deletedCount、ignoredImageIds 和 remainingCount。

#### Scenario: 删除一张图片
- **WHEN** imageIds 仅包含一个属于指定足迹的有效 imageId
- **THEN** 系统移除该图片并清理其原图和缩略图，返回 deletedCount=1

#### Scenario: 批量删除混合图片ID
- **WHEN** imageIds 同时包含有效、重复、无效、已删除或属于其他足迹的 ID
- **THEN** 系统删除全部有效图片，忽略其余 ID，并在响应中报告实际删除数、忽略 ID 和剩余图片数

#### Scenario: 删除最后一张图片
- **WHEN** 删除请求包含足迹仅剩的一张图片
- **THEN** 删除成功，足迹保留，remainingCount=0

#### Scenario: 重试已经完成的删除
- **WHEN** 客户端再次提交已经删除的 imageId
- **THEN** 请求仍成功，该 ID 出现在 ignoredImageIds 且 deletedCount 不包含该 ID

#### Scenario: 删除请求未提供图片ID
- **WHEN** imageIds 缺失或去重后为空
- **THEN** 返回 code=400，且不修改足迹或图片文件

### Requirement: 足迹图片分页查询
系统 SHALL 通过 `POST /api/information/image/list` 根据 informationId、page 和 size 按图片集合顺序分页返回公开图片数据。每条记录 SHALL 包含 imageId、fileName、数字 fileSize、contentType、width、height、createTime、thumbnailUrl 和 originalUrl，不得包含内部文件标识或图片 Base64。

#### Scenario: 分页查询足迹图片
- **WHEN** 提供存在的 informationId 和有效分页参数
- **THEN** 返回 total、page、size 和当前页 records，顺序与足迹图片集合一致

#### Scenario: 足迹没有图片
- **WHEN** informationId 对应足迹存在但 images 为空
- **THEN** 返回 code=200、message="该足迹没有图片"、total=0 和空 records

#### Scenario: 分页查询的足迹不存在
- **WHEN** informationId 对应足迹不存在
- **THEN** 返回 code=400，提示记录不存在

### Requirement: 永久图片访问
系统 SHALL 为每张图片提供公开、不鉴权、不签名、不过期的永久相对 thumbnailUrl 和 originalUrl。访问时系统 MUST 验证 informationId 与 imageId 的归属关系，并分别返回缩略图或原图二进制及正确 Content-Type。图片内容创建后不得在相同 imageId 下被替换，且 imageId 不得复用。

#### Scenario: 查看缩略图
- **WHEN** 客户端访问某图片的 thumbnailUrl 且图片属于指定足迹
- **THEN** 返回缩略图二进制、正确 Content-Type、长期公开缓存指令和 `X-Content-Type-Options: nosniff`

#### Scenario: 查看原图
- **WHEN** 客户端访问某图片的 originalUrl 且图片属于指定足迹
- **THEN** 返回原图二进制、正确 Content-Type、`Content-Disposition: inline`、长期公开缓存指令和 `X-Content-Type-Options: nosniff`

#### Scenario: 图片不属于指定足迹
- **WHEN** informationId 与 imageId 的归属关系不成立
- **THEN** 返回 HTTP 404，不泄露该 imageId 是否存在于其他足迹

#### Scenario: 图片已删除
- **WHEN** 客户端访问已删除图片且服务端没有可用资源
- **THEN** 返回 HTTP 404；客户端已经缓存的副本不属于服务端可撤回范围

### Requirement: 缩略图处理
系统 SHALL 为每张有效图片同步生成最大边界 `320×320`、保持原始比例、不裁剪且不放大小图的缩略图。JPEG 缩略图 SHALL 修正 EXIF Orientation 并输出 JPEG；PNG 缩略图 SHALL 输出 PNG 并保留透明通道。原图字节 MUST 保持不变。系统 SHALL 在原图、缩略图或元数据任一步失败时使整个创建或新增操作失败并清理本次已产生的文件。

#### Scenario: 生成横向或纵向缩略图
- **WHEN** 上传可正常解码且任一边超过 320 像素的 JPEG 或 PNG
- **THEN** 生成的缩略图位于 320×320 边界内并保持原始比例

#### Scenario: 上传较小图片
- **WHEN** 原图宽高均不超过 320 像素
- **THEN** 系统不得为生成缩略图而放大图片

#### Scenario: JPEG 包含方向标记
- **WHEN** JPEG 依赖 EXIF Orientation 正确显示方向
- **THEN** 缩略图按正确方向输出，原图字节保持不变

#### Scenario: PNG 包含透明通道
- **WHEN** PNG 原图包含透明区域
- **THEN** PNG 缩略图保留透明通道

#### Scenario: 缩略图处理失败
- **WHEN** 原图存储、尺寸读取、EXIF 处理、缩略图生成、缩略图存储或元数据写入任一步失败
- **THEN** 操作失败且不遗留本次原图、缩略图或不完整图片元数据

### Requirement: 图片大小与容量约束
系统 SHALL 将单个上传文件限制为 50MB，并将 multipart 请求总大小限制为 55MB，以容纳单文件之外的表单开销。系统 SHALL 不限制 JPEG 或 PNG 的宽度、高度、总像素数，也不限制单个足迹的累计图片字节数，但图片数量 MUST NOT 超过 50。

#### Scenario: 文件超过单文件上限
- **WHEN** 上传文件大小超过 50MB
- **THEN** 返回 code=400，提示上传文件大小超过限制

#### Scenario: 请求超过总大小上限
- **WHEN** multipart 请求总大小超过 55MB
- **THEN** 返回 code=400，提示上传文件大小超过限制

#### Scenario: 超高分辨率文件未超过字节限制
- **WHEN** JPEG 或 PNG 可正常解码且文件与请求字节数未超限，但其宽高或总像素很大
- **THEN** 系统不得仅因分辨率或总像素拒绝该文件

### Requirement: 历史足迹数据清理
系统 SHALL 提供只能由用户在维护窗口手工执行的一次性清理脚本，支持预览和正式执行两种模式。脚本 SHALL 收集全部历史 `image_metadata` 记录引用的旧 GridFS 文件，只删除这些关联文件和全部历史 `image_metadata` 记录，MUST NOT 清空或影响共享 GridFS bucket 中的其他文件。新版本 SHALL 从空足迹数据开始，不读取或迁移旧单图结构。

#### Scenario: 预览清理范围
- **WHEN** 用户以预览模式执行脚本
- **THEN** 脚本报告足迹数和关联文件数，但不修改任何元数据或文件

#### Scenario: 正式清理历史足迹
- **WHEN** 旧版本已经停止写入且用户以正式模式执行脚本
- **THEN** 脚本尝试删除全部历史足迹关联文件，删除全部 `image_metadata` 记录，并报告成功、缺失和失败的文件数及失败 ID

#### Scenario: GridFS 中存在其他业务文件
- **WHEN** 共享 bucket 包含没有被历史足迹元数据引用的文件
- **THEN** 清理脚本不得删除或修改这些文件

#### Scenario: 新版本启动
- **WHEN** 历史清理完成后部署新版本
- **THEN** 系统仅使用新 images 数组结构，不提供旧单图字段兼容逻辑

## REMOVED Requirements

### Requirement: 历史元数据迁移
**Reason**: 本次数据模型从单图记录切换为原图与缩略图成对的有序图片集合，用户已决定放弃全部历史足迹和历史图片，不再迁移旧时间或行政区字段。

**Migration**: 在维护窗口停止旧版本写入，先预览并正式执行新的历史足迹清理脚本，只删除历史足迹引用的 GridFS 文件和全部 `image_metadata` 记录，然后部署新版本。
