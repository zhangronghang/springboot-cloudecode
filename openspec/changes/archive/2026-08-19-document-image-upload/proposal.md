## Why

图片上传与备注管理功能已实现并投入使用，但其技术设计与分步实现清单此前散落在已移除的 superpowers 计划文档中。仓库需要一份 OpenSpec 体系下的技术文档，为后续维护与扩展提供可追溯的设计上下文。

## What Changes

- 新增 `design.md`：记录现有图片上传模块的技术设计（存储架构、分层、关键决策与权衡）
- 新增 `tasks.md`：记录已完成的实现步骤清单，作为功能实现轨迹的索引
- 不改动任何代码、接口行为或既有主 spec（`openspec/specs/image-upload/spec.md` 已覆盖行为规格）

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

（无）

## Impact

- 仅文档变更，无代码、API、依赖或系统行为影响
- 主 spec `image-upload` 保持不变（本 change 声明 `skip_specs: true`）
