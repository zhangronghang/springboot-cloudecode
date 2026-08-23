# image_metadata 迁移执行说明

本目录的 `migrate-image-metadata.js` 仅在用户通过 `mongosh` 手工执行时运行。应用启动不会执行该脚本。

## 1. 执行前准备

1. 停止旧版本对 `claude_code.image_metadata` 的写入，或安排维护窗口。
2. 确认目标数据库为 `claude_code`，collection 为 `image_metadata`。
3. 先备份元数据 collection（示例命令按实际 MongoDB 地址和备份目录调整）：

```powershell
mongodump --uri="mongodb://127.0.0.1:27017/claude_code" --collection=image_metadata --out="D:\backup\claude_code-before-information-migration"
```

GridFS 文件不会被该脚本修改，因此无需由本脚本迁移。

## 2. Dry-run（默认且必须先执行）

脚本中的 `DRY_RUN` 默认值为 `true`。从项目根目录执行：

```powershell
mongosh "mongodb://127.0.0.1:27017/claude_code" --file scripts/mongodb/migrate-image-metadata.js
```

请核对输出：

- 输出的目标数据库必须为 `claude_code`，collection 必须为 `image_metadata`。
- 每条 `[DRY RUN]` 记录只会转换旧 `uploadTime`、回填缺失的 `createTime`，以及为缺失行政区字段填入空字符串。
- `跳过记录` 会列出无法解析的 `uploadTime`；这些记录不会被脚本改写，需人工处理后再运行。

## 3. 正式执行

确认 dry-run 输出无误后，打开 `migrate-image-metadata.js`，将底部的：

```javascript
var DRY_RUN = true;
```

临时改为：

```javascript
var DRY_RUN = false;
```

使用同一条 `mongosh` 命令执行。完成后将该开关改回 `true`，避免下次误写入。

## 4. 结果核对与幂等性

再次以 `DRY_RUN = true` 执行脚本。已成功迁移的记录不会再次产生 `[DRY RUN]` 更新；汇总中的 `planned` 应为 `0`。仍被跳过的记录需要根据输出的 `_id` 和原始时间值人工修复。

## 5. 回滚

若需要回滚，先停止新版本写入，再恢复第 1 步的备份。例如：

```powershell
mongorestore --uri="mongodb://127.0.0.1:27017/claude_code" --drop "D:\backup\claude_code-before-information-migration\claude_code\image_metadata.bson"
```

恢复后再部署旧版本应用。请先在非生产环境验证实际备份路径和恢复命令。
