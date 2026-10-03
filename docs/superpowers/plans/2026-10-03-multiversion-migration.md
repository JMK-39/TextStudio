# TextStudio / RealmControl 多版本迁移实施计划

> 使用 superpowers:executing-plans 在当前会话连续执行，各项目单独验证。

**Goal:** TextStudio 先完成架构迁移和 1.21.1 适配，再执行 RealmControl。

**Architecture:** 核心的 Stonecutter 单源码节点架构，按 loader 分离构建脚本，共享资源、命名和依赖解析。26.1.2 在对应核心可用前只预留扩展点。

**Tech Stack:** Gradle 9.8.0、Stonecutter 0.9.8、MDG 2.0.148、Java 21/25。

**Spec:** ../specs/2026-10-03-multiversion-migration-design.md

## Global Constraints

- 保留工作区已有修改，最低核心 26.10.3；不推送、不发布，不操作用户游戏进程。
- JAR 命名 `<mod_id>-<loader>-<minecraft>-<mod_version>.jar`，输出 D:/NEWMODS。
- Forge 1.20.1 字节码 65，Mixin JAVA_17、refmap 和 MixinConfigs；NeoForge 使用独立 TOML，无 refmap。
- 依赖必须匹配当前 loader/MC，显式版本严格遵守，离线不访问 GitHub。

## Review Focus

- 同版本核心不同加载器必须正确分离。
- 旧核心文件名仅限 Forge 1.20.1 回退。
- 相对输出路径以仓库根目录解析。
- 样式 kf 字段经过存储和网络往返不得丢失。
- 客户端类不得阻止专用服务端启动。

### Task 1: TextStudio 构建迁移

**Files:** settings.gradle、stonecutter.gradle、build.forge.gradle、build.neoforge.gradle、gradle/kinetic-node.gradle、gradle/kinetic-core-dependency.gradle、versions/**、wrapper、资源 TOML、workflow。

**Interfaces:** 共用 `kineticConfigureReleaseJar(TaskProvider<Jar>)`；依赖脚本配置当前节点的 compile/runtime 和 kineticCoreReobf。

- [x] 保存已有 diff，建立工作分支和进度记录。
- [x] 运行原有检查，记录基线；新节点构建命令当前不存在，构成迁移前失败证据。
- [x] 按核心实际脚本迁移构建，保留架构例外，新增 Mixin 目标检查。
- [x] 执行 `gradlew :1.20.1-forge:build --offline`，检查最终 JAR 元数据、refmap、字节码和资源。

### Task 2: TextStudio 1.21.1

**Files:** versions/1.21.1-neoforge/gradle.properties、src/main/java/**，样式 codec 与版本回归测试。

**Interfaces:** 继续复用核心 KineticNetwork 和 GUI v2；条件源码使 Forge 版本保持原有行为。

- [x] 编译 1.21.1，记录真实 API 差异。
- [x] 针对序列化行为先建立往返回归，按实际签名适配 SavedData、文本和 Mixin。
- [x] 执行 `gradlew buildAll --offline`，两个节点都经过完整检查。
- [x] 检查两个最终 JAR，执行开发运行冒烟；记录能验证和未验证的功能。

### Task 3: RealmControl

**Files:** RealmControl 自己的构建、资源、版本差异源码与迁移报告。

**Interfaces:** 复用 Task 1 已验证的通用附属脚本，仅替换模组信息与架构例外。

- [x] TextStudio 验收后保存 RealmControl 基线，独立调查差异。
- [x] 完成其 1.20.1 构建迁移并运行完整检查。
- [x] 完成其 1.21.1 适配并运行 buildAll、JAR 验证与冒烟。
- [x] 自查差异，分别记录构建结果和 26.1.2 前置条件。

## 执行记录

2026-10-03：用户审阅设计后要求“开始”。按当前会话执行；不再为可逆本地操作重复请求授权。使用原目录的独立工作分支，保留用户未提交修改；现有校验加真实构建用于构建配置迁移，行为适配补充有意义的回归。

2026-10-03：TextStudio 双节点完整离线构建和产物检查通过；1.21.1 主菜单及真实样式 codec 夹具通过；1.20.1 按用户授权以 PCL2 原设置启动进入标题界面。详见 docs/multiversion-migration-report.md。

2026-10-03：RealmControl 双节点完整离线构建及 JAR 检查通过；PCL2 原设置启动两个已有版本进入标题/主菜单，未修改内存。实际 1.21.1 夹具验证旧 NBT/组件规则、默认值与组件删除、部分附魔、名称/Lore、信标 Mixin 和 SavedData。复核问题均修复并完成运行回归。详见 ../RealmControl/docs/multiversion-migration-report.md。两个附属的 26.1.2 节点仍预留未启用，等待对应核心依赖后继续移植与验收。
