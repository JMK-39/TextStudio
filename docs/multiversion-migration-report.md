# 多版本迁移验收（2026-10-03）

## 支持状态

| 节点 | 构建 | 已有游戏版本启动 |
| --- | --- | --- |
| 1.20.1-forge | 完整离线 build 通过 | PCL2 启动 OTHERWORLD CLASH，标题界面加载完成 |
| 1.21.1-neoforge | 完整离线 build 通过 | 已进入主菜单，运行时样式测试通过 |
| 26.1.2-neoforge | 预留节点，未启用 | 缺少匹配的 KineticCore 产物，未冒用其他节点依赖 |

`gradlew buildAll --offline` 同时生成 `D:/NEWMODS/textstudio-forge-1.20.1-26.10.3.jar` 和 `textstudio-neoforge-1.21.1-26.10.3.jar`。两个节点使用共享的上海日期版本号、Java 21、Gradle 9.8.0、Stonecutter 0.9.8 和 MDG 2.0.148。

## 变化

- 构建拆分为 controller、加载器脚本和共用节点脚本；核心依赖按 Minecraft/loader 精确筛选。固定依赖优先本地库；核心专用仓库避免被旧命名 flatDir 文件绕过。显式错节点核心及旧命名 NeoForge 回退负例均拒绝。
- SavedData、组件序列化、皮肤、输入框、聊天队列和举报界面按 1.21.1 的接口移植；旧聊天存档名、数据结构、配置和语言文件保持。
- 1.21.1 使用样式 codec 保存原有 `kf` 效果字段。样式变换避免污染全局 `Style.EMPTY`。实体参数建议的辅助缓存移出 Mixin 包。
- 保留用户迁移前的 Java 21 / 最低核心 26.10.3 意图；基线 diff 保存在本地 `.gradle/migration/preexisting.patch`。

## 验证证据

- `.gradle/migration/build-final.log`：两个完整 build、架构检查、最终核心引用检查和 Mixin 目标检查通过。
- 两个节点各检查 26 个 Mixin，0 个新问题、1 个已知问题；核心引用检查各 0 个问题。
- `gradle/Verify-ReleaseJar.ps1` 检查两个产物：各 143 个类，字节码 65、对应 TOML、正确 Mixin 配置；Forge 的 refmap、JAVA_17 和清单均存在。最低核心范围仍为 `[26.10.3,)`。测试夹具不进入发布 JAR。
- 与迁移前 Forge JAR 对比：语言及其他资源内容一致；变化为清单、展开后的 TOML、字体 Mixin 列表。新增 codec/构造访问器和移出 Mixin 包的辅助类替换两个旧内部辅助类，类清单因此有合理差异。
- `checkPackedStyleCodec` 在两个节点通过：普通/旧样式、所有效果位、额外字段及畸形字段。
- 1.21.1 的可选 `runtimeValidationJar` 在实际加载器内输出 `TEXTSTUDIO_RUNTIME_VALIDATION_PASS`，验证 Style / Component JSON / 网络 codec 往返及 EMPTY 隔离。测试后夹具移至游戏版本的 `codex-migration-backup`。
- 按用户后续授权使用 F:/game 下已有版本，无单独下载游戏。1.20.1 最终使用 PCL2 原有启动设置，未修改内存。日志显示资源加载完成、title_screen 注册和 Game took 39.904 seconds to start；TextStudio Mixin 正常应用。旧附属 JAR 备份在版本目录的 `codex-migration-backup`。

## 已知问题与边界

`ChatClientMixins$ReportingContextTweaks.textstudio_chat$disableReportingContext` 注入的 `ReportingContext.hasReporting` 在两个目标版本均不存在。登记于节点 `known_mixin_issues`，保留迁移前行为，未借迁移修改举报功能。

未声称完成多人同步、玩家实际头像渲染、跨会话聊天历史恢复或专用服务端启动测试。构建检查与 codec 测试不能代替这些场景。整合包日志另有现有模组/资源警告，未扩大本次修复范围。

26.1.2 需对应核心发布 JAR 后再启用并继续实际移植；当前预留不代表支持。未推送或发布 Release。

## 1.21.1 聊天滚动条修复（15:31 验证）

原版 ChatComponent 的两次带深度 fill 绘制压在自定义滑块上，造成灰色重叠。仅当自定义滚动条已注册、开启且存在可滚动历史时跳过这两次绘制；其余情况保留原版，Forge 注入保持不变。

buildAll --offline 与发布包检查通过。在 PCL2 的现有 1.21.1 世界确认全宽橙色、悬停黄色，无灰边；拖动历史与滚轮同步正常，日志无 Mixin 加载错误。只读代码审查无 Critical/Important 问题。产物已替换该实例的 TextStudio JAR，旧包保存在 codex-migration-backup/textstudio-scrollbar-20261003。
