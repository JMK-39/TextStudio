# TextStudio 多版本迁移设计（已批准）

## 目标与顺序

按用户要求，以 `D:/IDEAWork/KineticCore` 当前实现为准，先准备 TextStudio 的多版本架构，再从 1.21.1 开始适配。目标版本为 Forge 1.20.1、NeoForge 1.21.1、NeoForge 26.1.2。TextStudio 的当前阶段验收后，才迁移 `D:/IDEAWork/RealmControl`；两个项目分别验证。

本设计将当前阶段解释为：完成 1.20.1 的架构迁移和 1.21.1 的代码适配，同时为 26.1.2 准备构建扩展点。26.1.2 完整适配仍属于目标，但启用构建需要对应版本的 KineticCore。不能用 1.21.1 的核心 JAR 冒充 26.1.2 的依赖，也不能把预留节点称为已支持版本。

## 已核实的基线

- 核心使用 Stonecutter 0.9.8、ModDevGradle 2.0.148、Gradle 9.8.0；已启用 `1.20.1-forge` 和 `1.21.1-neoforge`。`26.1.2-neoforge` 在核心 settings 中仍是注释，README 标为适配中。
- 本地 `D:/NEWMODS` 有 `kineticcore-forge-1.20.1-26.10.3.jar` 和 `kineticcore-neoforge-1.21.1-26.10.3.jar`，本次检查没有发现 26.1.2 的核心产物。
- TextStudio 目前仍是单版本 ForgeGradle 构建。工作区已有六个修改文件：两个发布 workflow、build.gradle、gradle.properties、reobf 检查脚本、mods.toml。RealmControl 也有这六类修改。迁移必须保留已有意图，尤其是 Java 21 和 KineticCore 最低版本 `26.10.3`。
- TextStudio 有聊天和字体两套 Mixin 配置，没有发现 `src/test`。网络已使用核心 `KineticNetwork`，无需另建附属网络体系。
- 核心交接文档原本只要求架构迁移；本次用户明确追加 1.21.1 适配，该部分以用户请求为准。

## 方案比较

1. **采用核心的 Stonecutter 单源码结构（推荐）。** 最大限度复用核心已验证的构建方式；加载器包名由替换规则处理，真实 API 差异使用条件源码。版本节点独立配置、独立构建、统一输出。
2. 各版本复制一套源码和构建。初期简单，但字体效果、聊天和配置逻辑会逐渐分叉，后续维护成本高。
3. 只修改现有 ForgeGradle 和文件名。无法满足一次构建 Forge 与 NeoForge，也没有为 26.1.2 建立可靠扩展点。

选择方案 1；不引入额外加载器抽象框架，不重写核心已有的 API。

## 构建边界

- `settings.gradle`：声明 Stonecutter 和 foojay，创建可构建版本节点，`vcsVersion='1.20.1-forge'`。
- `stonecutter.gradle`：声明两种 MDG 插件、复用核心的 Forge→NeoForge 替换、提供 `buildAll`，聚合所有已启用节点的完整 `build`。
- `build.forge.gradle`：Legacy Forge 开发运行、Mixin 注解处理器与 refmap、最终 `reobfJar`。
- `build.neoforge.gradle`：NeoForge 开发运行、Mojang 命名的普通 `jar`，无需 Forge 重混淆。
- `gradle/kinetic-node.gradle`：共用版本号、toolchain、资源处理、输出命名与通用检查。移除核心自己的 API/Javadoc/GUI 回归任务。
- `gradle/kinetic-core-dependency.gradle`：集中管理当前节点对应的核心依赖，避免 Forge 和 NeoForge 解析逻辑复制。
- `versions/<minecraft>-<loader>/gradle.properties`：保存 MC、加载器、Java 和资源包格式；共用模组信息保留在根 properties。
- wrapper、daemon JVM、`.gitignore` 和两个 workflow 对齐核心。旧根 `build.gradle` 由 controller 和 loader 脚本替代。

1.20.1 和 1.21.1 都输出 Java 21 字节码。26.1.2 的扩展点必须允许节点使用 Java 25，而非把所有节点锁死为 Java 21。26.1 系列要求 Java 25，并调整了 GUI 渲染接口，依据 [NeoForge 官方迁移说明](https://neoforged.net/news/26.1release/)。未完成移植前，26.1.2 节点保持未启用，不进入 `buildAll` 或 Release。

## 核心依赖解析

按节点 loader 和 MC 精确筛选 JAR；新命名识别 `<loader>-<minecraft>`，旧命名仅作为 Forge 1.20.1 兼容回退，不对 NeoForge 回退。

本地搜索顺序：Gradle 用户目录上一级的 `libs` → 核心 properties 指定的输出目录 → `../KineticCore/build/libs`。比较数值版本而非文件名字符串；同版本优先明确标记当前节点的新命名产物。第三方固定版本依赖使用本地库优先策略。

无显式版本时，在线比较本地版本与 GitHub 稳定 Release；只有远端具有当前节点对应资产且版本更新才使用远端。离线不请求 GitHub。有 `-Pkineticcore_version=<version>` 时严格遵守指定版本，不再被更高本地版本覆盖。没有当前节点依赖时说明缺少哪个 loader/MC，禁止拿其他节点的 JAR 补位。

最低核心版本和运行依赖范围保留 `26.10.3` / `[26.10.3,)`。

## 1.21.1 代码适配

保留原有配置、命令、文字效果、聊天持久化和同步行为。优先使用核心公开 API；必须接触原版字体、聊天框宿主的既有例外按核心架构检查规则保留。

先以实际编译和 Mixin 检查确定差异，再逐项调整：

- 加载器注解和生命周期包名由 Stonecutter 替换处理；实际签名差异用条件源码。
- `Component.Serializer` 的 JSON 读写使用对应版本所需的注册表上下文；历史记录格式保持兼容，不能丢失格式和效果信息。
- Forge 1.20.1 的 `Style.Serializer` 保留现有 JSON 注入；1.21.1 的样式序列化按实际 codec 接口实现同一 `kf` 字段的读写。禁止仅移除旧 Mixin 导致字体样式在网络或存盘时丢失。
- `SavedData` 的工厂、加载和保存方法按版本适配，保持旧存档名 `textstudio_chat_history` 和数据结构。
- 字体渲染、聊天行、头像皮肤、社交界面、文本输入和网络包长度相关 Mixin 核实方法描述符及注入点。
- 辅助类和可调用静态方法位于 Mixin 包之外，避免 `IllegalClassLoadError`。

## 资源与产物

- Forge 使用 `META-INF/mods.toml`，NeoForge 使用 `META-INF/neoforge.mods.toml`；分别展开当前节点的版本范围，只打包对应文件。
- Forge Mixin 保留 `JAVA_17`、refmap 和清单 `MixinConfigs`。NeoForge 在生成资源时调整 Java 级别、删除 refmap 字段并在 TOML 声明两份 Mixin 配置。
- 资源包格式随版本节点展开，不在源资源中写死 15。
- 最终命名：`<mod_id>-<loader>-<minecraft>-<mod_version>.jar`。
- 例如：`textstudio-forge-1.20.1-26.10.3.jar`、`textstudio-neoforge-1.21.1-26.10.3.jar`；26.1.2 完成后为 `textstudio-neoforge-26.1.2-26.10.3.jar`。
- 输出保留 `D:/NEWMODS`；CI 使用仓库根目录 `build/release`，一次收集全部已启用版本的 JAR。发行日期按 Asia/Shanghai 生成，同一次构建版本号一致。

## 验收与后续顺序

1. 首先验证 1.20.1 的完整 `build`：架构、核心最终 JAR 方法引用、Mixin 目标、最终资源和字节码检查。
2. 完成并验证 1.21.1 的完整 `build`，覆盖样式序列化往返、聊天历史恢复、网络同步及版本特有 Mixin。
3. `buildAll` 一次输出两个已支持节点的独立 JAR；离线构建确认依赖来自本地/已有缓存。检查不能只依赖 `compileJava`。
4. 对照迁移前产物的类/资源清单；1.20.1 的语言文件、配置格式和功能保持一致。
5. 开发客户端及专用服务端冒烟检查使用独立运行目录；用户整合包由用户自行测试，不启动或终止用户的现有游戏进程。不能把构建通过称为游戏功能已验证。
6. 达到当前阶段验收后，开始 RealmControl：先独立调查，再复用构建框架并适配自身差异。不得在 TextStudio 未通过时批量改第二个项目。
7. 26.1.2 的核心依赖和接口可用后，再启用该节点、完成文本/渲染/输入移植，将其加入三个版本的 `buildAll` 验收。当前缺失的核心适配不能被静默扩大成修改 KineticCore 的任务。

## 工作区保护

保留已有未提交修改，不 reset、不 stash 丢弃。实现前保存基线差异，完成后报告新增改动与验证结果。只处理用户指定的两个附属，不推送、不发布 Release，也不修改用户游戏目录。当前文件是审阅设计，尚未实施构建或源码迁移。

## 后续执行授权

用户要求使用 F:/game/异界战斗幻想/.minecraft/versions 中已有版本启动验证，不另行下载游戏。后续指定 PCL2，保留原有内存设置，仅切换版本启动；该指令覆盖上述原先不操作游戏目录/进程的限制。仅安装有备份的本次产物，正常退出自己发起的测试客户端，不强行结束其他游戏。
