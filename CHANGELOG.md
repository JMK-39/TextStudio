2026年10月24日 — 26.10.24 — Buttons at the top, standard slots / 按钮置顶与统一物品格
- Font effect editor: the text field, Copy, Prefix, Stop and Save share the top row; the “实时预览” and “ID” labels are gone and the editor moved up; Add Preset sits in the left header; Cancel reads Back.
- 文字特效编辑：输入框、一键复制、前缀、截断与保存同在顶行；去掉“实时预览”和“ID”文字，编辑区上移；新增预设移到左侧顶栏；“取消”改为“返回”。

2026年10月23日 — List spacing / 列表间距

- The colorful-text editor's effect list centres each name in its row, 2 px from the row's edges.
- 炫彩文本编辑器的效果列表把名称垂直居中，与行边框上下各留 2 像素。

---

2026年10月20日 — Editor entry / 编辑器入口
- Chat-copy text selection uses the exact visible content clip, including fractional scrolling, and starts only with the left button. The GUI fixture checks all clipped edges and the first/last visible pixels.
- 聊天复制文本选择与可见裁剪区域完全一致，平滑滚动时也对齐；仅左键开始拖选，并补充四边裁剪及首末可见像素的界面回归检查。

- Text editors use one white outer frame and gray-white section dividers. Preset rows and popup menus keep their interactive outlines; the chat copy view no longer has a second gold frame.
- 文本编辑页统一使用白色外框及灰白区域分隔线，预设交互行与弹出菜单保留边框；聊天复制页移除第二层金色边框。

- Open the colorful-text and color-preset editor through F6's TextStudio configuration. Font commands show name-style help and retain rename, effect, toggle and clear operations; editor-opening command aliases were removed.

- 炫彩文本与颜色预设编辑器通过 F6 的 TextStudio 配置打开。字体命令显示名称样式帮助，保留改名、效果、开关及清除操作；移除打开编辑器的命令入口。

---

2026年10月16日 — 编辑画布与预设删除

- 聊天搜索框加宽 40 像素，聊天浏览与字体编辑页使用 640×360 画布及 4 像素返回内距。
- 字体编辑器可用 Delete 删除当前预设，保留最后一个预设的原有限制。
- 字体效果与颜色预设左侧列表改为更紧凑的行高，列表按钮间距固定为 2 点，编号保持独立白色显示。

---

2026年10月13日 — Chat copy toolbar background / 聊天复制工具栏背景

- The chat copy canvas's Back button, search field and match buttons sit on the same dark panel as the chat history instead of directly over the world. The match count is vertically centred with the buttons.

- 聊天复制画布的返回按钮、搜索框与匹配切换按钮与聊天记录共用同一深色面板，不再直接显示在游戏画面上；匹配计数与按钮垂直居中。

---

2026年10月10日 — Navigation placement / 返回按钮位置

- Cancel in the font preset editor and Back in the font guide and chat copy canvas now sit at the top left. Preset lists, previews, effect controls and the chat history frame keep their existing layout.
- Header titles and searches leave room for navigation; long labels and chat match counts scroll within their own space.
- Navigation button labels use white text.

- 字体预设编辑器的“取消”、字体指南与聊天复制画布的“返回”现在位于左上角。预设列表、预览、效果控件与聊天历史边框保持现有布局。
- 顶部标题与搜索为返回控件留出空间；过长标签与聊天匹配计数在各自范围内滚动显示。
- 返回类按钮的文字统一使用白色。

---

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
