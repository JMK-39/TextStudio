2026年10月04日 13时42分 — 26.10.4

- Added NeoForge 26.1.2 support (Java 25, KineticCore 26.10.3+). Text effects, the effect editor and its live preview, chat timestamps, sender heads, chat history and author names work on 26.1.2.
- On 26.1.2, effect text in menus, chat and tooltips is drawn glyph by glyph through 26.1's new GUI text pipeline, with correct letter spacing.
- Long labels in the effect editor and the guide page (common in English) now stay inside their space and scroll like vanilla buttons instead of covering the input boxes next to them.
- Checked on 26.1.2: style codec round trips, joining a world, chat and the editor in English and Chinese.

- 新增 NeoForge 26.1.2 支持（Java 25，需要 KineticCore 26.10.3+）。文字特效、特效编辑器及其实时预览、聊天时间戳、发送者头像、聊天记录和作者名称均可在 26.1.2 中使用。
- 26.1.2 中，菜单、聊天和悬浮提示里的特效文字通过 26.1 新的界面文字流程逐字绘制，字间距正确。
- 特效编辑器和说明页中过长的标签（英文下较常见）现在会留在自己的区域内，并像原版按钮一样滚动显示，不再覆盖旁边的输入框。
- 已在 26.1.2 中检查：样式编解码往返、进入世界、聊天，以及中英文下的编辑器界面。

---

2026年10月04日 — Language key validation / 语言键一致性检查

- Require identical authored English/Chinese keys and string values in source, version overrides and packaged resources; generated formatting keys are rejected during builds.

- 强制检查源码、版本覆盖与最终资源的中英文完整键名一致、值为字符串；构建禁止派生格式语言键。

---

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
