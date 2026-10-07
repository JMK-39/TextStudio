2026年10月08日 — Chat toolbar spacing / 聊天工具栏间距

- The chat toolbar keeps a gap from the frame instead of touching it.

- 聊天工具栏与边框保留间距，不再紧贴边框。

---

2026年10月07日 00时50分 — Guide header / 指南标题

- The guide page's "Quick Use" heading ends before the Back button in its row instead of running under it.

- 指南页面的"快速使用"标题在同一行的返回按钮之前结束，不再压到按钮下面。

---

2026年10月06日 08时35分 — 26.10.6

- The guide page's preset names and its "Current" line, and the editor's selected preset ID, scroll inside their own space instead of running to the frame or under the Cancel and Save buttons.
- With KineticCore 26.10.6, long labels show an ellipsis and scroll on hover, and on 26.1.2 the preset rows keep their right border.

- 指南页面的预设名称与"当前"一行，以及编辑器中选中的预设 ID，改为在各自区域内滚动，不再延伸到边框或压到"取消""保存"按钮下面。
- 使用 KineticCore 26.10.6 时，过长的标签显示省略号并在悬停时滚动；26.1.2 上预设行的右边框不再缺失。

---

2026年10月04日 13时42分 — 26.10.4

- Added NeoForge 26.1.2 support (Java 25, KineticCore 26.10.3+). Text effects, the effect editor and its live preview, chat timestamps, sender heads, chat history and author names work on 26.1.2.
- On 26.1.2, effect text in menus, chat and tooltips keeps correct letter spacing.
- Long labels in the effect editor and the guide page (common in English) now stay inside their space and scroll like vanilla buttons instead of covering the input boxes next to them.

- 新增 NeoForge 26.1.2 支持（Java 25，需要 KineticCore 26.10.3+）。文字特效、特效编辑器及其实时预览、聊天时间戳、发送者头像、聊天记录和作者名称均可在 26.1.2 中使用。
- 26.1.2 中，菜单、聊天和悬浮提示里的特效文字保持正确字间距。
- 特效编辑器和说明页中过长的标签（英文下较常见）现在会留在自己的区域内，并像原版按钮一样滚动显示，不再覆盖旁边的输入框。

---

2026年10月03日 15时35分 — 26.10.3

- Added NeoForge 1.21.1 support alongside Forge 1.20.1, using matching KineticCore 26.10.3+.
- Adapted text-style saving and synchronization for 1.21.1 and fixed the vanilla chat scrollbar overlapping the custom scrollbar.
- Minecraft 26.1.2 is not yet supported.

- 新增 NeoForge 1.21.1 支持，同时保留 Forge 1.20.1；使用对应版本的 KineticCore 26.10.3+。
- 适配 1.21.1 文字样式的保存与同步，修复原版聊天滚动条与自定义滚动条重叠。
- 尚不支持 Minecraft 26.1.2。

---

历史记录（原记录未标注时间）

- Fixed style-prefix placeholders appearing as boxes with hex numbers, and taking up width, in item tooltips, toasts and other text that is measured or wrapped before it is drawn. The prefix control characters are now zero-width empty glyphs, so they are never shown or counted in any text path, and a name with a long prefix is no longer split apart when a tooltip wraps.

- 修复样式前缀的占位符在物品提示、Toast 等先量宽、换行再绘制的文字里显示为带十六进制编号的方框并占用宽度的问题。前缀控制字符现在是零宽度的空字形，任何文字路径里都不会显示或计入宽度，带长前缀的名字也不会再在提示框换行时被拆开。
