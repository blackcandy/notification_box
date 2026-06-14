# 通知滤盒 (NotifBox)

一个仿 [通知滤盒](https://play.google.com/store/apps/details?id=com.catchingnow.np) 的 Android 应用：
**记录所有到达的通知**（即使被划掉也能回看），并用**规则**自动过滤垃圾通知。
AI 智能过滤为后续迭代预留（见下方 Roadmap）。

技术栈：Kotlin + Jetpack Compose + Room，原生 `NotificationListenerService`。

## 工作原理

系统在用户授予「通知访问权限」后绑定 `NotifListenerService`。每条到达的通知都会被
快照、经 `RuleEngine` 评估，然后落库到 Room。UI 分三个标签页展示：收件箱（未过滤）、
已过滤、规则管理。

```
通知到达 → NotifListenerService → RuleEngine.evaluate(规则)
        → Room (notifications 表, filtered 标记) → Compose UI (Flow 实时刷新)
```

## 项目结构

```
app/src/main/java/com/notifbox/
├── NotifBoxApp.kt              Application，持有 Room/Repository 单例
├── data/                      Room 实体、DAO、数据库、Repository
│   ├── NotificationEntity.kt  捕获的通知快照
│   ├── FilterRule.kt          过滤规则（包含 / 正则 / 包名）
│   └── ...
├── filter/RuleEngine.kt       无状态规则评估器（已带单元测试）
├── service/NotifListenerService.kt  核心：监听系统通知
├── ui/                        Compose 界面（收件箱 / 已过滤 / 规则）
└── util/NotificationAccess.kt 检查并跳转到「通知访问」系统设置
```

## 构建运行

本机当前未检测到 Android SDK / Android Studio。需要先安装：

1. 安装 **Android Studio**（最新稳定版即可，自带 JDK 17 与 Gradle）。
2. 用 Android Studio 打开本目录（`J:\Projects\notification_box`），等待 Gradle 同步
   （首次会自动下载依赖）。
3. 连接真机或启动模拟器（**minSdk 26 / Android 8.0+**），点击 Run。
4. 首次启动后，进入「规则」页 → 「前往授权」，在系统设置里为本应用打开**通知访问权限**。

命令行构建（已配置 Gradle Wrapper，需本机有 JDK 17）：

```powershell
.\gradlew.bat assembleDebug      # 打包 debug APK
.\gradlew.bat test               # 运行 RuleEngine 单元测试
```

> 若用命令行，还需创建 `local.properties` 指向 SDK：
> `sdk.dir=C\:\\Users\\spens\\AppData\\Local\\Android\\Sdk`

## Roadmap

- [x] 通知监听 + 历史记录（Room）
- [x] 规则过滤（关键词 / 正则 / 包名）+ 单元测试
- [x] 命中规则时从通知栏移除（`cancelNotification`，设置里可开关）
- [x] 历史保留期与自动清理（DataStore 设置项，服务连接时按保留天数清理）
- [x] 通知详情页（点列表项查看完整内容 + 命中的规则）
- [x] 按应用分组（带数量）+ 关键词搜索
- [x] 后台保活：前台服务（`KeepAliveService`）+ 开机自启（`BootReceiver`）+ 电池白名单申请 + `requestRebind`
- [ ] **AI 智能过滤**：端上轻量文本分类模型，与 `RuleEngine` 并行打分
- [ ] 通知数量角标
```
