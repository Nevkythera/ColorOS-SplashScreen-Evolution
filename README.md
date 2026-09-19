# COS遮罩进化

> Module ID: `com.Nevkythera.ColorOSSplashScreenEvolution`

一个针对 ColorOS 的**启动遮罩（SplashScreen）还原与自定义模块**，把被 ColorOS
改写成 XML 预览图的开机画面还原为 Android 原生启动遮罩，并提供图标、背景与退出动画的自定义能力。

此项目使用AI辅助制作，虽能保证不包含人工输入的恶意代码，但无法保证AI产生的代码符合预期效果。

模块灵感源于[RestoreSplashScreen / 启动遮罩进化](https://github.com/GSWXXN/RestoreSplashScreen/tree/Compose)对SplashScreen的高度自定义展现的可能性。

## 免责声明

- 本模块需要 **LSPosed（支持 libxposed API 102）** 与 **Root** 环境，
  作用域必须勾选 `com.android.systemui` 与「系统框架（android）」。
- 本项目仅在 ColorOS 16.1 设备（一加Ace5至尊版）上测试，其它机型与系统版本请自行测试。
- **风险提示**，请勿在未备份的主力机上贸然使用。因使用本模块导致的任何后果由使用者自负。
- 部分功能需重启系统界面（设置页右上角）或重启设备后生效。

## 主要功能

### 图标

- 绘制图标圆角
- 缩小图标（不缩小 / 缩小全部图标）
- 替换图标获取方式（跟随主题图标包）
- 关闭截图覆盖（忽略应用自带启动图）
- 移除图标（隐藏启动遮罩上的全部图标，含底部品牌图）
- **Material 3 Expressive 几何形变加载动画**

### 背景

- 替换背景颜色：不替换 / Material You 动态取色 / 自定义颜色
- 颜色模式：浅色 / 暗色 / 跟随系统
- 自定义背景颜色（浅色、暗色分别设置）

### 动画

- 退出动画效果：（实验性）

### 杂项

- 热启动也适用启动遮罩（应用从后台恢复时同样显示启动遮罩，而非任务快照）（实验性）

### 应用配置

- 显示桌面图标
- 应用内语言切换（跟随系统 / 简体中文 / English）

### 关于

- 模块激活状态与框架信息
- Root 管理器与版本检测
- 设备信息、鸣谢与开源地址

## 构建

```bash
# 需要 Android SDK（platform 36 / build-tools 36.0.0）与 JDK 17+
# 版本号在 app/build.gradle.kts 的 defaultConfig，
# 并需同步 app/src/main/resources/META-INF/xposed/module.prop

gradle :app:assembleRelease
```

## 参考与致谢

本项目为 **GPL-3.0** 许可。以下项目在思路、公式或素材层面被参考：

| 项目 | 作者 | 用途 |
|---|---|---|
| [RestoreSplashScreen / 启动遮罩进化](https://github.com/GSWXXN/RestoreSplashScreen/tree/Compose) | GSWXXN | 启动遮罩 Hook 思路与灵感 |
| [MCGA](https://github.com/JiaGuZhuangZhi/MCGA) | JiaGuZhuangZhi | 设置界面与组件结构参考 |
| [Telegram](https://github.com/DrKLO/Telegram) | Telegram FZ-LLC | 实现粒子尘埃效果 |
| [Aghajari/ThanosEffect](https://github.com/Aghajari/ThanosEffect) | Amir Hossein Aghajari | 上述效果的复刻与参考 |

## 许可

[GPL-3.0](LICENSE)
