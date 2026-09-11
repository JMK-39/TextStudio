# Text Studio

[English](#english) | [简体中文](#简体中文)

## English

### Overview

A client-facing text presentation and chat enhancement toolkit with configurable font effects, animated text styles, presets, preview tools, message history improvements, timestamps, copying, duplicate-message handling and presentation controls.

The project is designed around in-game administration. Where a feature changes shared gameplay data or server rules, the server remains authoritative; client-only presentation features stay local to the client. Configuration screens use KineticCore's UI and configuration infrastructure.

### Key Features

- Dynamic text effects including gradients, RGB-style coloring, wave, shine, typing, shake and glitch-style presentation.
- Preset management, live text preview and reusable formatting workflows.
- Improved chat history, timestamps, copying and duplicate-message consolidation.
- Chat presentation controls for long messages, scrolling and visual layout.
- Configuration through KineticCore with a unified in-game interface.

### Requirements and Compatibility

| Type | Dependency |
|---|---|
| Required | Minecraft 1.20.1 |
| Required | Minecraft Forge 47+ |
| Required | KineticCore 26.9.8+ |
| Optional | None |

### Access and Configuration

- Open the KineticCore configuration center with its configured F6 entry and select **Text Studio**.
- Server-owned settings are saved by the server and synchronized where the feature requires client awareness.
- Client-only presentation settings remain local.
- Individual feature areas document their own data/configuration paths below.
- Search, list selection, item/entity inspection, tooltips and return/navigation controls reuse KineticCore UI components where available.

## Detailed Feature Reference

### Text Effects

#### Overview

**Text Effects** is a KineticCore-based dynamic text renderer and player-name styling module. It packages color, gradient, palette, wave, sweep, typewriter, transform, jitter and glitch behavior into reusable presets and provides in-game guidance, visual editing, copy output and name-style controls.

#### Key Features

- 640×360 KineticCore visual effect editor with live preview.
- Guide and preset workflow through F6 and `/kt font` / `/kt font guide`.
- Compact invisible control payloads; ordinary text bypasses the dynamic parser.
- Rainbow, palette, color pulse, wave, sweep, typewriter, glyph motion/transform, bounce, jitter and glitch rendering paths.
- Player display-name customization with preset IDs and style toggles; advanced movement/glitch name effects are permission-restricted.
- Compatibility for anvil rename, sign input, player lists, entity selectors and command suggestions.
- Style serialization and font-rendering integration through the Minecraft text pipeline.
- Bounded caches and fast-path checks for high-frequency rendering.
- Presets stored in `config/kineticcore/textstudio_effects.toml`.

### Feature Reference
#### GUI and Editors
| Item | Description |
|---|---|
| **Live Preview** | Type text to preview |
| **palette** | Pick colors directly or enter an exact 6-digit HEX color. |

#### Commands
| Item | Description |
|---|---|
| **font** | Player name style settings |
| **rainbow** | Toggle the client global rainbow override |
| **bold** | Toggle the client global bold override |
| **strike** | Toggle the client global strikethrough override |
| **jitter** | Toggle the client global physical jitter override |
| **glitch** | Toggle the client global cyber glitch override |
| **effect** | Set the name effect; advanced motion, jitter, and glitch effects are automatically stripped for non-authors |
| **clear** | Clear the custom name for this world/save and restore the default name |
| **test** | Generate a virtual showcase board with multiple stress-test effects |
| **set** | Change your display name for this world/save; Chinese and English are supported |
| **dynamic** | Use /kt font to open the Text Effects guide, choose a preset, enter text, and copy it. |
| **author** | Player name style settings |
| **effect** | Set the dynamic effect ID for your name (1-60) |
| **clear** | Clear the custom name and restore the original name |
| **set** | Set the target preset ID for the global dynamic tag ＄#＾ |
| **dynamic** | Open the Text Effects guide to select a preset, enter text, and copy it in one click. |
| **editor** | Open the Text Effects visual effect editor |

#### Editable Options
- Color & Glow
- Motion & Animation
- Glitch & Performance
- Palette
- Effect category: %s
- Advanced Glyphs
- Visual Layers

#### Command Syntax Index

- `/kt font rename <name>`
- `/kt font name effect <ID>`
- `/kt font name toggle <option>`
- `/kt font name clear`
- `/kt font guide`

#### Data Paths
Primary configuration/data paths:

- `config/kineticcore/textstudio_effects.toml`

### Chat Presentation

#### Overview

**Chat Presentation** is a modern chat enhancement module for this project. It removes restrictive vanilla limits and adds practical tools for long-running multiplayer sessions.

#### Key Features

- Extended chat and command length, configurable up to a much larger limit than vanilla.
- Large client chat history.
- Bounded server-side history persistence.
- Draggable chat scrollbar.
- Millisecond-precision timestamps.
- Repeated-message compaction with counters.
- Player skin heads in chat history.
- Dedicated chat copy canvas.
- Optional late-stage chat signature stripping.
- Separation between server rules and local client display preferences.

#### Configuration

```text
config/kineticcore/chat.toml
```

### Feature Reference
#### Config Details
| Item | Description |
|---|---|
| **Chat Enhancements** | Server-authoritative chat length, history storage, and signature rules. Both singleplayer and multiplayer save through the active server; editing requires permission level 2. |
| **Compact Chat (Stacking)** | Merges consecutive identical messages and appends a count, reducing spam from system messages, mod errors, or repeated player messages. |
| **Draggable Scrollbar** | Shows a draggable scrollbar on the right side of chat for quickly browsing a large message history. |
| **Show Player Avatars** | Renders real-time skin avatars before chat messages. Prioritizes premium skins with automatic offline-skin fallback based on UUID. |
| **Max History Lines** | Maximum history lines retained by the server for each player and synchronized to clients as the display limit. Valid range is 100–100000; server storage also has a per-player size budget. |
| **Save Chat History** | Controls whether the server persists player chat and input history across sessions. When disabled, no new persistent history is recorded or synchronized; existing saved history is not deleted automatically. |
| **Max Chat Length** | Maximum characters accepted by the chat box, packets, and commands. Vanilla uses 256; clients and servers should use matching values. |
| **Strip Chat Signatures** | Controls whether the server strips chat signatures before broadcasting and synchronizes that choice to clients for the matching report and insecure-chat UI behavior. |
| **Enable Timestamps** | Shows a local timestamp with millisecond precision [HH:mm:ss.SSS] before every chat message. |
| **Chat Client Preferences** | Affects only this client's display and interaction preferences. These settings are stored locally and never written to the server config. |

#### GUI and Editors
| Item | Description |
|---|---|
| **Back** | Exit capture mode and return to chat screen. |
| **Capture** | Open virtual canvas for scrolling, selecting, and copying full chat history. |

#### Config Defaults
| Key | Default |
|---|---|
| `enable_chat_heads` | `true` |
| `enable_chat_history_saving` | `true` |
| `enable_compact_chat` | `true` |
| `enable_draggable_scrollbar` | `true` |
| `enable_timestamp` | `true` |
| `max_chat_history_lines` | `10000` |
| `max_chat_length` | `16384` |
| `strip_chat_signatures` | `true` |

#### Data Paths
Primary configuration/data paths:

- `config/kineticcore/chat.toml`

### Building from Source

- Minecraft: `1.20.1`
- Java: `17`
- ForgeGradle: `6.0.24`
- Gradle: the project is pinned to the `8.1.1` Wrapper; do not import it with Gradle 9 directly.
- Local development JARs are controlled by `local_libs_dir` and can be overridden in `gradle.properties` or with a project property.
- Typical build command: `gradlew.bat build` on Windows or `./gradlew build` on Linux/macOS.
- Development and release artifacts use `textstudio` as the current project identifier.

## 简体中文

### 模组定位

面向客户端文本显示与聊天体验的增强工具，提供字体效果、动态文本样式、预设与预览，以及聊天历史、时间戳、复制、重复消息合并和显示控制。

本项目以游戏内管理为核心。涉及共享玩法数据、世界规则或服务器规则的功能由服务端权威处理；仅影响显示的客户端功能保持本地生效。配置界面统一使用 KineticCore 提供的 GUI 与配置基础设施。

### 主要功能

- 支持渐变、RGB 风格颜色、波浪、扫光、打字、抖动、故障等动态文本效果。
- 支持效果预设、实时文本预览与可复用格式配置。
- 增强聊天历史、时间戳、复制与重复消息合并。
- 支持长消息、滚动与聊天界面显示相关设置。
- 通过 KineticCore 统一配置界面进行管理。

### 运行环境与兼容

| 类型 | 依赖 |
|---|---|
| 必需 | Minecraft 1.20.1 |
| 必需 | Minecraft Forge 47+ |
| 必需 | KineticCore 26.9.8+ |
| 可选 | 无 |

### 打开方式与配置

- 使用 KineticCore 配置中心对应的 F6 入口，选择 **Text Studio**。
- 服务端规则由服务端保存，并在需要时同步给客户端。
- 纯显示类客户端设置只在本地生效。
- 各功能自己的配置/数据路径在下方详细功能说明中列出。
- 搜索、列表选择、物品/实体信息读取、悬浮提示、返回与导航等操作尽可能复用 KineticCore GUI 组件。

## 完整功能参考

### 文本效果

#### 模组定位

**Text Effects / 文字特效** 是基于 KineticCore 的动态文字渲染与玩家名称样式模块。它把颜色、渐变、调色板、波浪、扫光、打字机、位移、抖动、故障等效果封装为可复用预设，并提供游戏内教程、可视化编辑、复制输出、名称设置与客户端全局样式开关。

#### 主要功能

- **640×360 可视化特效编辑器**：在 KineticCore 统一画布中编辑预设、预览文字、颜色与动态效果。
- **游戏内教程与预设选择**：可通过 F6 配置入口或 `/kt font` / `/kt font guide` 打开说明与选择界面。
- **紧凑隐藏控制数据**：特效信息以不可见控制数据附着在文本中，普通文本直接走快速路径，不进入特效解析器。
- **动态效果体系**：包含彩虹、调色板、颜色脉冲、波浪、扫光、打字机、字形运动/变换、跳动、抖动与故障等渲染能力。
- **玩家显示名称**：支持中文/英文名称、预设效果 ID、彩虹、粗体、删除线等开关；高级位移、抖动、故障类名称效果具有权限限制。
- **输入与显示兼容**：铁砧重命名、告示牌、聊天相关文本、玩家列表、实体选择器、命令建议等路径会读取文字特效样式。
- **序列化与渲染兼容**：对 Style 序列化、Font、GuiGraphics、StringDecomposer 等文字管线提供兼容处理。
- **性能保护**：控制标记和自定义数据采用有界缓存，普通文本不做不必要的动态效果解析。
- **预设文件**：效果预设保存在 `config/kineticcore/textstudio_effects.toml`。

### 完整功能参考

#### 界面操作与编辑器说明

| 项目 | 说明 |
|---|---|
| **实时预览** | 输入要预览的文字 |
| **palette** | 支持快速取色，也可输入 6 位 HEX 精确颜色。 |

#### 命令功能说明

| 项目 | 说明 |
|---|---|
| **font** | 玩家名称样式设置 |
| **rainbow** | 开关当前客户端的全局彩虹流光覆写 |
| **bold** | 开关当前客户端的全局加粗覆写 |
| **strike** | 开关当前客户端的全局删除线覆写 |
| **jitter** | 开关当前客户端的全局物理震颤覆写 |
| **glitch** | 开关当前客户端的全局赛博撕裂覆写 |
| **effect** | 设置名称动态效果；普通玩家会自动剔除位移、抖动、故障等高级效果 |
| **clear** | 清除当前存档中的自定义名称，恢复默认名称 |
| **test** | 生成一块包含多种极限测试特效的虚拟展示板 |
| **set** | 修改当前存档中的玩家显示名称，支持中文和英文 |
| **dynamic** | 使用 /kt font 打开文字特效教程，选择预设、输入文字并一键复制。 |
| **author** | 玩家名称样式设置 |
| **effect** | 设置名称的动态特效 ID (1-60) |
| **clear** | 清除自定义名称，恢复原始名称 |
| **set** | 设置全局动态标签 ＄#＾ 的目标预设 ID |
| **dynamic** | 打开文字特效教程，选择预设、输入文字并一键复制。 |
| **editor** | 打开文字特效可视化编辑器 |

#### 可编辑字段、模式与分类索引

- 颜色与发光
- 位移与动画
- 故障与演出
- 调色板
- 效果分类：%s
- 字形高级
- 视觉叠层

#### 命令语法索引

- `/kt font rename <名称>`
- `/kt font name effect <ID>`
- `/kt font name toggle <开关>`
- `/kt font name clear`
- `/kt font guide`

#### 配置与数据路径

主要配置/数据路径：

- `config/kineticcore/textstudio_effects.toml`

### 聊天显示

#### 模组定位

**Chat Presentation** 是 本项目中的现代化聊天增强模块，目标是解除原版聊天长度和历史记录限制，并为长时间服务器游玩提供更实用的聊天回溯、复制和防刷屏能力。

#### 主要功能

- **超长聊天与指令**：突破原版 256 字符限制，默认可将聊天/指令最大长度提高到 16384 字符。
- **大容量聊天历史**：客户端可保存远高于原版的历史行数，默认上限 10000 行。
- **服务器历史持久化**：本机/集成服务器侧为玩家保存有大小限制的聊天历史，并优先淘汰最旧记录。
- **可拖拽滚动条**：聊天界面右侧增加可视化滚动条，便于快速翻阅大量历史。
- **高精度时间戳**：可为每条消息显示 `[HH:mm:ss.SSS]` 时间。
- **重复消息合并**：连续相同消息自动叠楼并显示次数，减少系统消息或重复文本刷屏。
- **聊天头像**：玩家消息前可以显示 8×8 皮肤头像，并根据 UUID 恢复历史头像信息。
- **聊天复制画布**：聊天界面提供复制入口，可打开独立画布查看并复制历史文本。
- **签名剥离**：可在服务器广播阶段移除聊天签名，同时保留服务器侧聊天事件处理流程。
- **客户端与服务端配置分离**：服务端规则由服务器保存，纯客户端显示偏好保留本地控制。

#### 配置文件

```text
config/kineticcore/chat.toml
```

常见设置：

- `max_chat_length`
- `max_chat_history_lines`
- `enable_draggable_scrollbar`
- `enable_timestamp`
- `enable_compact_chat`
- `strip_chat_signatures`
- `enable_chat_heads`

### 完整功能参考

#### 配置项详细说明

| 项目 | 说明 |
|---|---|
| **聊天增强** | 服务器权威的聊天长度、历史存储与签名规则。单人和多人都由当前服务器保存；需要 OP 2 级权限才能编辑。 |
| **聊天叠楼 (合并消息)** | 连续出现的相同消息会合并显示并附加次数，减少系统消息、模组报错或玩家复读造成的刷屏。 |
| **可拖拽滚动条** | 在聊天界面右侧显示可用鼠标拖拽的滚动条，方便快速翻阅大量历史消息。 |
| **显示玩家头像** | 在聊天栏玩家消息前实时渲染皮肤头像。优先获取正版皮肤，并根据离线 UUID 自动生成占位头像。 |
| **历史记录行数** | 服务器最多为每位玩家保留的聊天历史行数，并同步给客户端作为历史显示上限。可设置 100–100000；服务器还会限制每位玩家的历史存储体积。 |
| **保存聊天记录** | 控制服务器是否跨会话保存玩家聊天记录与输入历史。关闭后不会记录或同步新的持久化历史，已有历史数据不会被主动删除。 |
| **聊天最大长度** | 聊天输入框、网络包和指令的最大字符数。原版限制为 256；联机时客户端与服务器应使用一致的值。 |
| **剥离聊天签名** | 由服务器决定是否在广播前剥离聊天签名，并同步给客户端隐藏相应举报与不安全聊天提示。 |
| **启用时间戳** | 在每条聊天消息前显示精确到毫秒的本地时间戳 [HH:mm:ss.SSS]。 |
| **聊天客户端偏好** | 仅影响当前客户端显示与操作体验，直接保存在本地，不会写入服务器配置。 |

#### 界面操作与编辑器说明

| 项目 | 说明 |
|---|---|
| **返回** | 关闭抓取模式并返回聊天界面。 |
| **抓取历史** | 开启虚拟画布，支持全量历史记录的滚动选取与复制。 |

#### 配置键与默认值

| 配置键 | 默认值 |
|---|---|
| `enable_chat_heads` | `true` |
| `enable_chat_history_saving` | `true` |
| `enable_compact_chat` | `true` |
| `enable_draggable_scrollbar` | `true` |
| `enable_timestamp` | `true` |
| `max_chat_history_lines` | `10000` |
| `max_chat_length` | `16384` |
| `strip_chat_signatures` | `true` |

#### 配置与数据路径

主要配置/数据路径：

- `config/kineticcore/chat.toml`

### 从源码构建

- Minecraft：`1.20.1`
- Java：`17`
- ForgeGradle：`6.0.24`
- Gradle：项目固定使用 `8.1.1` Wrapper，请不要使用 Gradle 9 直接导入。
- 默认本地依赖目录由 `local_libs_dir` 控制，可在 `gradle.properties` 或命令行参数中覆盖。
- 常用构建命令：`gradlew.bat build`（Windows）或 `./gradlew build`（Linux/macOS）。
- 生成的开发/发布文件以 `textstudio` 作为当前工程标识。
