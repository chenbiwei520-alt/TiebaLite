# <p align="center">Tieba Lite</p>
<div align="center">
    <a href="LICENSE">
        <img alt="License: GPL-3.0" src="https://img.shields.io/badge/License-GPL_3.0-blue.svg">
    </a>
    <img alt="Platform" src="https://img.shields.io/badge/platform-Android-green">
    <img alt="API" src="https://img.shields.io/badge/API-24%2B-orange">
    <br/>
    <br/>
    <p>极简的贴吧<strong>第三方</strong>客户端 · HyperOS 风格重构版</p>
</div>

## 关于本分支

基于 [0ranko0P/TiebaLite](https://github.com/0ranko0P/TiebaLite) 的延续开发，将全部界面迁移到
**Miuix（HyperOS 设计语言）** 组件体系：

- **Miuix 0.9.4**（`miuix-ui / preference / icons / blur / squircle`）承载界面组件与主题，
  `ThemeController` 统一管理亮暗与动态取色
- **顶部渐进模糊**：全部页面统一为 miuix-blur 的 `progressiveTextureBlur`（AGSL），
  通过离屏 GPU 探针探测设备 AGSL 真实可渲染性，不可用时自动降级为平面顶栏；
  顶栏模糊带与 miuix 规范对齐（状态栏 + 单行栏高，渐变在栏下缘收干净）
- **液态玻璃底栏**：[AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)
  实现的悬浮底部导航与下拉菜单
- **Squircle 连续曲率**：卡片、按钮、图片容器全面使用 squircle 形状（API 33+ 原生，以下自动回退）
- 帖子卡片、设置页、引导页、对话框、拾色器等均已按 Miuix 规范重写

> 包名已更换为 `com.Jeaceny.tieba.post`，可与原版并存安装。

## 分支说明

| 分支 | 用途 |
|---|---|
| `main` | 里程碑合入（跟随上游） |
| `miuix` | **日常开发分支**，HyperOS 重构在此进行 |

## 构建

1. 克隆仓库
```shell
git clone https://github.com/chenbiwei520-alt/TiebaLite.git
cd TiebaLite
git checkout miuix
```

2. 配置应用签名（非必须）

编辑 `signing.properties.example` 填写密钥库路径、密钥别名与密码，保存为 `signing.properties`。

3. 开始构建
```shell
./gradlew :app:assembleRelease
```

成功后，构建产物在 `app/build/outputs/apk/release/` 下。

环境要求：JDK 17+，Android Studio（AGP 9.2 / Kotlin 2.3 / compileSdk 37）。

## 致谢

- [Huanchengfly/TiebaLite](https://github.com/Huanchengfly/TiebaLite) — 原始项目
- [0ranko0P/TiebaLite](https://github.com/0ranko0P/TiebaLite) — 本仓库的直接上游
- [miuix](https://github.com/miuix-kotlin-multiplatform/miuix) — HyperOS 设计语言 Compose 多平台组件库
- [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) — 液态玻璃效果
- [Starry-OvO/aiotieba](https://github.com/Starry-OvO/aiotieba) — Asynchronous I/O Client for Baidu Tieba
- [n0099/tbclient.protobuf](https://github.com/n0099/tbclient.protobuf) — 百度贴吧客户端 Protocol Buffers 定义文件合集

## 说明

**本软件及源码仅供学习交流使用，严禁用于商业用途。**

本项目为 GPL-3.0 协议的开源软件，使用前请阅读 [LICENSE](LICENSE)。
