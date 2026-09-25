# Text Studio

[English](#english) | [简体中文](#chinese)

<a id="english"></a>

## English

Text Studio combines animated text authoring, player display-name styling, and a more capable chat interface. It provides a visual preset editor and copyable effect text for pack authors, alongside chat history, selection/copy tools, timestamps, and player avatars for everyday use.

### Installation and entry points

| Component | Requirement |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.4.2 or newer |
| KineticCore | 26.9.20 or newer; required |

Use Text Studio with KineticCore on the client and server for the complete multiplayer feature set. Rendering and local display preferences run on the client; names, chat limits, and persistent history involve the server.

Press **F6**, KineticCore's default configuration-center key, to open Text Studio's text-effects and chat pages. You can also use `/kt font` to open the effect guide directly.

### Build and reuse text effects

The editor combines effects rather than restricting each preset to a single animation:

| Effect family | Available examples |
| --- | --- |
| Color | Rainbow, custom palette, pulse, fade, neon flicker, shimmer, sparkle |
| Motion | Wave, bounce, shake, swing, pendulum, orbit, ripple, drift |
| Special animation | Typewriter, reverse erasing, glitch, signal loss, heartbeat, ECG sweep, note bounce |
| Extra rendering | Outline, glow, chromatic offsets, trails, extrusion, per-glyph scaling |

Preview your own text while adjusting supported speed, amplitude, width, color, or intensity controls. Presets can be created, selected, duplicated, reset, and deleted. Saving persists the preset collection and closes the editor.

The guide lets you select a preset and copy complete styled text, a prefix, or a stop code. A prefix affects following text until a stop code appears. The editor can copy effect parameters together with the preview text, making it useful for composing a specific visual result.

Effect codes use compact invisible control characters. Use the guide/editor copy buttons instead of manually typing markup. A copied preset reference depends on the corresponding local preset definition; a custom-parameter copy carries its effect parameters with the text.

Glow makes the text body full-bright and adds a stable outline. A large bloom halo depends on the shader pack rather than being guaranteed by the glow setting.

### Example workflow

1. Run `/kt font editor` or open the visual editor through F6.
2. Select a preset, enter preview text, and adjust the effect controls.
3. Copy the full result, or combine prefix + text + stop for a styled segment.
4. Save preset changes if you want to reuse them later.
5. Paste the generated text into a text field supported by the mod and inspect the rendered result.

### Player display-name commands

These commands act on the executing player; they are not account-name changes.

| Command | Action |
| --- | --- |
| `/kt font` | Open the effect guide |
| `/kt font guide` or `/kt font help` | Open the same guide |
| `/kt font editor` | Open the visual preset editor |
| `/kt font name` or `/kt font name help` | Show display-name command help |
| `/kt font name set <name>` | Set your custom display name |
| `/kt font rename <name>` | Alias for setting your display name |
| `/kt font name effect <id>` | Select a valid configured name-effect preset |
| `/kt font name toggle rainbow <true/false>` | Toggle the rainbow name flag |
| `/kt font name toggle bold <true/false>` | Toggle bold names |
| `/kt font name toggle strike <true/false>` | Toggle strikethrough names |
| `/kt font name clear` | Clear the custom display name |

Names are limited to 24 visible Unicode code points after supported formatting controls are removed. Accepted visible characters include Han characters, Latin letters, digits, spaces, `_`, and `-`. Reserved names and names already used by another online player are rejected as applicable.

Some name effects have a specific restriction: `jitter`, `glitch`, and the special author effect are limited to the author UUIDs built into the mod. Advanced components of ordinary players' selected name presets are filtered. Operator status alone does not grant that author identity. This restriction concerns player-name styling, not the ability to explore the visual editor.

### Chat interface and history

- Increase chat/command input length beyond vanilla's 256-character limit.
- Retain a larger chat history and restore saved chat plus input history when rejoining a world.
- Display millisecond timestamps in `[HH:mm:ss.SSS]` format.
- Combine consecutive identical messages with a repetition counter.
- Show a draggable scrollbar and skin avatars beside player messages.
- Open the chat copy canvas to scroll, select, and copy text from the full retained history.

The server chat page controls the following settings; defaults are shown here, rather than assumed for every server:

| Setting | Default | Configurable range/behavior |
| --- | --- | --- |
| Chat and command length | 16,384 | 256–32,767 |
| Retained history lines | 10,000 | 100–100,000; also subject to storage size limits |
| Persistent history | Enabled | Disabling stops new recording/synchronization without deleting existing saved history |
| Signature stripping | Enabled | Removes chat signatures before message broadcast |

Signature stripping changes how chat messages are sent; it does not remove server logs or guarantee that conversations cannot be recorded. Timestamps, duplicate compaction, scrollbars, and avatars are separate client-local preferences.

### Persistence and performance

| Location | Stored data |
| --- | --- |
| `config/kineticcore/textstudio_effects.toml` | Local effect presets and rendering performance settings |
| `config/kineticcore/chat.toml` | Server chat settings and local display preferences, on their respective instance |
| World saved data `textstudio_chat_history` | Per-player persistent chat and input history in the overworld data storage |
| Player NBT | Custom display name, selected effect, and style flags |

History storage has an additional **8 MiB per-player encoded-data budget** and discards the oldest chat records when limits are exceeded. Displayed history is therefore bounded even when the line limit is large.

Rendering settings include `performance_refresh_ms`, `performance_max_animated_glyphs`, and `performance_extra_pass_budget`. They let pack authors control animation update frequency, animated character count, and extra rendering passes. If many animated labels are visible, adjust these alongside the visual effects.

Presets are local definitions. Ship the intended preset configuration with a client pack when you rely on shared preset IDs. Player name state is synchronized separately from the local preset collection.

[Back to language selection](#text-studio)

<a id="chinese"></a>

## 简体中文

Text Studio 将动态文字制作、玩家显示名称样式和聊天界面增强整合在一起。整合包作者可使用可视化预设编辑器制作并复制特效文本，普通玩家也能使用历史记录、选取复制、时间戳和聊天头像等功能。

### 安装与入口

| 组件 | 要求 |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.4.2 或更新版本 |
| KineticCore | 必需，26.9.20 或更新版本 |

完整多人功能需要客户端和服务端配合安装 Text Studio 与 KineticCore。文字渲染和本地显示偏好在客户端运行；名称、聊天限制和历史持久化涉及服务端。

按 **F6** 打开 KineticCore 配置中心中的文字特效与聊天页面。F6 是默认按键，也可用 `/kt font` 直接打开特效指南。

### 制作与复用文字效果

编辑器可以组合多种效果，一个预设不必局限于单一动画：

| 效果类别 | 示例 |
| --- | --- |
| 颜色 | 彩虹、自定义调色板、脉冲、淡入淡出、霓虹闪烁、流光、星尘 |
| 位移 | 波浪、弹跳、抖动、摇摆、钟摆、环绕、涟漪、浮游 |
| 特殊动画 | 打字机、反向擦除、故障、信号丢失、心跳、心电扫描、音符弹跳 |
| 额外渲染 | 描边、发光、色差、残影、立体挤出、单字缩放 |

输入自己的预览文本，即可边调整速度、幅度、宽度、颜色或强度等参数边观察效果。预设支持新增、选择、复制、重置和删除；保存会持久化整套预设并关闭编辑器。

指南中可选择预设，复制完整特效文本、前缀或停止码。前缀会持续作用于后续文字，直到遇到停止码。编辑器还可把当前效果参数与预览文本一起复制，便于保留特定视觉效果。

特效控制码使用紧凑的不可见字符。建议使用指南或编辑器的复制按钮，无需手写标记。按预设 ID 复制的文本依赖本地对应预设定义；携带自定义参数的复制结果则把参数嵌入文本。

发光会使文字主体保持全亮，并添加稳定轮廓。更大范围的朦胧 Bloom 由光影包决定，不能仅凭发光开关保证出现。

### 使用示例

1. 执行 `/kt font editor`，或通过 F6 打开可视化编辑器。
2. 选择预设，填写预览文字，调整所需效果。
3. 复制完整结果，或用“前缀 + 文本 + 停止码”拼接局部特效。
4. 如果以后还要复用，保存预设修改。
5. 将结果粘贴到模组支持的文本输入位置，检查实际渲染效果。

### 玩家显示名称命令

这些命令作用于执行命令的玩家，用于修改显示名称，不修改账号名称。

| 命令 | 用途 |
| --- | --- |
| `/kt font` | 打开特效指南 |
| `/kt font guide` 或 `/kt font help` | 打开同一指南 |
| `/kt font editor` | 打开可视化预设编辑器 |
| `/kt font name` 或 `/kt font name help` | 查看名称命令帮助 |
| `/kt font name set <名称>` | 设置自己的显示名称 |
| `/kt font rename <名称>` | 设置显示名称的简写入口 |
| `/kt font name effect <编号>` | 选择配置中有效的名称特效预设 |
| `/kt font name toggle rainbow <true/false>` | 开关名称彩虹样式 |
| `/kt font name toggle bold <true/false>` | 开关粗体 |
| `/kt font name toggle strike <true/false>` | 开关删除线 |
| `/kt font name clear` | 清除自定义显示名称 |

名称在去除支持的格式控制码后，最多为 24 个可见 Unicode 码点。允许汉字、拉丁字母、数字、空格、下划线和连字符；保留名称和其他在线玩家已使用的名称会按规则拒绝。

部分名称效果有明确的身份限制：`jitter`、`glitch` 和特殊作者效果仅供模组内置作者 UUID 使用。普通玩家选用名称预设时，其中的高级效果会被过滤。OP 权限本身不会获得作者身份。该限制针对玩家名称样式，不妨碍浏览和使用可视化编辑器。

### 聊天界面与历史

- 将聊天和命令输入长度扩展到原版 256 字符以上。
- 保留更多聊天内容，重新进入世界时恢复已保存的聊天和输入历史。
- 显示 `[HH:mm:ss.SSS]` 格式的毫秒级时间戳。
- 合并连续相同消息，并显示重复次数。
- 提供可拖拽滚动条和玩家皮肤头像。
- 通过聊天复制画布滚动、选取并复制完整保留历史中的文字。

以下由服务端聊天页面控制，表中数值是默认值，服务器可另行调整：

| 设置 | 默认值 | 范围或行为 |
| --- | --- | --- |
| 聊天与命令长度 | 16,384 | 256–32,767 |
| 历史保留行数 | 10,000 | 100–100,000，同时受存储大小限制 |
| 历史持久化 | 开启 | 关闭后不再新增记录或同步持久化历史，但不主动删除旧数据 |
| 签名剥离 | 开启 | 在广播消息前移除聊天签名 |

签名剥离改变消息发送方式，不会删除服务端日志，也不能保证聊天无法被记录。时间戳、重复合并、滚动条和头像则是独立的客户端本地偏好。

### 存储与性能

| 位置 | 保存内容 |
| --- | --- |
| `config/kineticcore/textstudio_effects.toml` | 本地文字预设与渲染性能设置 |
| `config/kineticcore/chat.toml` | 各自实例中的服务端聊天设置与客户端显示偏好 |
| 世界保存数据 `textstudio_chat_history` | 主世界数据存储中的逐玩家聊天与输入历史 |
| 玩家 NBT | 自定义显示名称、所选效果与样式标记 |

历史存储还设有每位玩家 **8 MiB 编码数据预算**，超过限制会优先丢弃最旧的聊天记录，因此较大的行数设置也不代表无限存储。

渲染配置包含 `performance_refresh_ms`、`performance_max_animated_glyphs` 和 `performance_extra_pass_budget`，用于控制动画更新频率、动态字符数量和额外渲染次数。画面中存在大量动态文字时，可结合视觉效果调整这些参数。

预设定义保存在本地。整合包如果依赖统一的预设 ID，应向客户端分发对应预设配置；玩家名称状态与本地预设集合采用不同的保存、同步路径。

[返回语言选择](#text-studio)
