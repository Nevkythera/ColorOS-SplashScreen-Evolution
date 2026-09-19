# COS遮罩进化（CSE — ColorOSSplashScreenEvolution）

ColorOS / OxygenOS 上的 **Android 原生启动遮罩（Splash Screen）还原模块**，
基于 **LSPosed / libxposed API 102**。

> **许可证：GPL-3.0**

> ## 🚨 接手本项目请先读交接文档（**不在仓库内**）
>
> 交接文档 `agent.md`、签名密钥等敏感文件已移出仓库，位于
> `CSE-private/`（与仓库同级）。那份文档包含构建环境、签名凭据、Hook 点位全图、
> **血泪踩坑清单**、持久化决策与全部关键约定。**不读会浪费几小时。**

---

## 快速开始

```bash
# 0) 确认 Android SDK 位置（应为 /opt/android-sdk）
cat local.properties

# 1) 版本号 +1 —— 编辑 app/build.gradle.kts 的 versionCode
#    并同步 app/src/main/resources/META-INF/xposed/module.prop

# 2) 编译
gradle :app:assembleRelease --offline

# 3) 对齐 + 签名（密钥不在仓库内，见 CSE-private/keys/cscr-new.jks）
BT=/opt/android-sdk/build-tools/36.0.0
$BT/zipalign -f 4 app/build/outputs/apk/release/app-release-unsigned.apk /tmp/aligned.apk
$BT/apksigner sign --ks ../../CSE-private/keys/cscr-new.jks --ks-key-alias cscr \
  --ks-pass pass:<STORE_PASSWORD> --key-pass pass:<STORE_PASSWORD> \
  --out releases/CSE-<version>-release-<MMDD-HHMM>.apk /tmp/aligned.apk

# 4) 验证签名（SHA-256 必须匹配文档中记录的指纹）
$BT/apksigner verify --print-certs releases/CSE-*.apk | head -6
```

## 目录说明

| 目录 | 内容 |
|---|---|
| `app/` | 应用与 Hook 模块源码 |
| `docs/` | 版本构建记录、签名说明（**交接文档 agent.md 已移出仓库**） |
| `CSE-private/`（与仓库同级） | **敏感文件**：签名密钥 `keys/`、交接文档 `agent.md`（不入仓库） |
| `releases/` | 历史 APK 归档 |

## 关键信息速查

| 项目 | 值 |
|---|---|
| 包名 | `com.Nevkythera.ColorOSSplashScreenEvolution` |
| 内部代号 | `CSE`（日志 TAG / 视图 tag / 偏好名前缀） |
| 当前版本 | versionName `0.3` / versionCode 见 `app/build.gradle.kts` |
| 作用域 | `com.android.systemui` + `android`（系统框架） |
| 签名 SHA-256 | `b5cddac72e84be1f34195fc0ad22944bbe2c5f2e675d17365ad427aaebe25ca0` |
| 最低版本 | LSPosed 支持 libxposed API 102 的版本；Android 15+（minSdk 35） |

## 参考与致谢（第三方思路 / 素材来源）

本项目为 **GPL-3.0** 许可。以下项目在**思路、公式与参数**层面被参考
（除另有说明外，未直接复制其代码）：

| 项目 | 作者 | 许可证 | 用途 |
|---|---|---|---|
| [Telegram Android](https://github.com/DrKLO/Telegram) 的「删除消息」尘埃粒子效果 | Telegram FZ-LLC | 见上游仓库 | 粒子消散效果的**原始设计与算法** |
| [Aghajari/ThanosEffect](https://github.com/Aghajari/ThanosEffect) | Amir Hossein Aghajari | 作者声明 MIT | 上述效果的复刻；本模块退出动画的**粒子更新公式与随机参数范围**取自其公开 README 与源码 |
| [GSWXXN/RestoreSplashScreen](https://github.com/GSWXXN/RestoreSplashScreen) | GSWXXN | 见上游仓库 | 启动遮罩 Hook 思路 |
| [JiaGuZhuangZhi/MCGA](https://github.com/JiaGuZhuangZhi/MCGA) | — | 见上游仓库 | 设置界面与组件结构参考 |

> ⚠️ **发布前必看**：`Aghajari/ThanosEffect` 仓库内**没有 LICENSE 文件**，
> 其 MIT 许可目前仅由作者在上游页面声明。正式发布前请**再次确认**，
> 并按其要求附上许可证全文与署名。

## 退出动画（粒子消散）说明

设置页 → 动画 → 退出动画，可选「默认」（系统原生 ripple）或「粒子消失」。
粒子消散按 Telegram 官方算法实现（见上方致谢）；渲染为**普通 View + Canvas**：

- 粒子数按 `perPx` 动态计算，上限 **40000**；
- 绘制按 (颜色 × alpha × 半径) 分桶后用 `Canvas.drawPoints` 批量提交；
- ⚠️ **不可用 GL/SurfaceView**：本视图位于起始窗口（StartingWindow）的
  `SplashScreenView` 之下，SurfaceView/GLSurfaceView 的独立表面无法创建（已实测失败）。
