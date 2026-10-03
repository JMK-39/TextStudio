2026年10月03日 15时35分 — 26.10.3

- Added NeoForge 1.21.1 support alongside Forge 1.20.1, using Java 21 and matching KineticCore 26.10.3+.
- Adapted text-style saving and synchronization for 1.21.1 and fixed the vanilla chat scrollbar overlapping the custom scrollbar.
- Both builds and targeted 1.21.1 runtime checks passed; complete multiplayer and chat-history scenarios have not all been tested.
- The 26.1.2 node is reserved and disabled; it is not a supported release.

- 新增 NeoForge 1.21.1 支持，同时保留 Forge 1.20.1；使用 Java 21 和对应版本的 KineticCore 26.10.3+。
- 适配 1.21.1 文字样式的保存与同步，修复原版聊天滚动条与自定义滚动条重叠。
- 两个版本构建及针对性的 1.21.1 运行检查通过；多人联机与聊天历史场景尚未全部测试。
- 26.1.2 节点仅预留、未启用，不代表已支持。

---

2026年10月02日 13时53分

- Enabled addon architecture validation; retained vanilla ChatScreen/EditBox and Font callback signatures required by the existing integrations.
- Recorded these callback boundaries only in the build configuration. No source-level warning suppression was added. The full build and final-JAR API verification passed.

- 接入附属架构检查，保留现有联动必需的原版 ChatScreen/EditBox 与 Font 回调签名。
- 回调边界仅在 build 配置中精确声明，未新增源码警告抑制。完整构建和最终 JAR API 检查通过。

---

历史记录（原记录未标注时间）

- Fixed style-prefix placeholders appearing as boxes with hex numbers, and taking up width, in item tooltips, toasts and other text that is measured or wrapped before it is drawn. The prefix control characters are now zero-width empty glyphs, so they are never shown or counted in any text path, and a name with a long prefix is no longer split apart when a tooltip wraps.

- 修复样式前缀的占位符在物品提示、Toast 等先量宽、换行再绘制的文字里显示为带十六进制编号的方框并占用宽度的问题。前缀控制字符现在是零宽度的空字形，任何文字路径里都不会显示或计入宽度，带长前缀的名字也不会再在提示框换行时被拆开。
