# TextStudio — 26.10.2

## English

- Enabled addon architecture validation; retained vanilla ChatScreen/EditBox and Font callback signatures required by the existing integrations.
- Recorded these callback boundaries only in the build configuration. No source-level warning suppression was added. The full build and final-JAR API verification passed.

## 中文

- 接入附属架构检查，保留现有联动必需的原版 ChatScreen/EditBox 与 Font 回调签名。
- 回调边界仅在 build 配置中精确声明，未新增源码警告抑制。完整构建和最终 JAR API 检查通过。
