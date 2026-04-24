# Somnil App (Android)

> 睡眠焦虑检测与干预 — Android 原生应用 (Jetpack Compose)

[![Android API](https://img.shields.io/badge/API-26%2B-blue.svg)]()
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-orange.svg)]()
[![License: Proprietary](https://img.shields.io/badge/License-Proprietary-red.svg)]()

**Somnil** 是一款睡眠焦虑检测与干预应用，通过单通道 EEG 设备实时监测睡眠脑电，自动识别焦虑相关微觉醒并触发个性化干预（声音/温度/香薰/智能家居设备联动）。

---

## 功能概览

### 🏠 首页 (HomeView)
- 设备连接状态实时显示 (BLE 连接 + RSSI 信号强度)
- 快速操作卡片：开始监测 / 查看报告 / 历史记录
- 当次 session 简要摘要 (监测中时)

### 📊 实时监测 (LiveMonitorView)
- EEG 波形实时显示 (30 秒窗口滚动)
- STA/LTA 阈值实时对比
- 睡眠阶段标签 (清醒/N1/N2/N3/REM)
- 焦虑事件实时告警

### 🎯 训练期 (TrainingView)
**3 天渐进式适应期，完成后模型自动个性化适配。**

- 圆形进度条 (0-100%，每日更新)
- 每日完成卡片 (显示当日 session 数 + 成功干预次数)
- 适应状态文字说明

### 📋 睡眠报告 (HistoryView)
- 历史 session 列表 (时间排序)
- 睡眠质量评分
- 焦虑事件趋势 (折线图)

### ⚙️ 设置 (SettingsView)
- **检测参数**：STA/LTA 阈值调节 (默认 1.5，范围 0.5-3.0)
- **检测灵敏度**：低 / 中 / 高
- **干预类型**：声音 / 温度 / 香薰 (开关控制)
- **Home Assistant MQTT**：配置 HA 主机、端口、用户名密码
- **小苹果 HA 设备**：支持灯光/开关/空调/加湿器/音箱联动

---

## 技术架构

### BLE 协议
- 连接管理、自动重连、State Restoration
- 支持固件 OTA 升级

### 数据处理
- `DataProcessor`：EEG 信号实时处理
- 频谱分析

### 干预集成
- 音频播放：白噪音/轻音乐/自然音
- MQTT：Home Assistant 集成

---

## 快速开始

### 前提条件
- Android Studio Hedgehog (2024.1)+
- Android API 26+ 设备
- Kotlin 1.9+

### 编译步骤

```bash
# 1. 克隆仓库
git clone https://github.com/v0id-byte/SomnilApp-Android.git
cd SomnilApp-Android

# 2. 打开项目
# Android Studio → Open → 选择 build.gradle.kts

# 3. 同步 Gradle
# Android Studio 自动同步，或:
./gradlew assembleDebug

# 4. 运行
# 连接 Android 设备 → Run 'app'
```

### 依赖项

| 包 | 用途 |
|---|------|
| Jetpack Compose | UI 框架 |
| Hilt | 依赖注入 |
| Kotlin Coroutines | 异步编程 |
| Ktor Client | 网络 (MQTT) |

---

## 项目结构

```
SomnilApp-Android/
├── app/
│   └── src/main/
│       ├── java/com/somnil/app/
│       │   ├── SomnilApp.kt              ← App 入口
│       │   ├── di/                       ← Hilt 依赖注入
│       │   ├── domain/model/             ← 数据模型
│       │   ├── service/                  ← BLE/数据处理服务
│       │   └── ui/
│       │       ├── screens/              ← 页面 Compose
│       │       ├── components/           ← 可复用组件
│       │       └── theme/                ← 主题配置
│       └── res/                          ← 资源文件
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 常见问题

### Q1: 设备无法连接？
1. 确保设备已开机且处于 BLE 广播模式
2. 检查手机蓝牙权限
3. 重启 App 和设备

### Q2: 训练期要多久？
- 最少 3 晚，每晚需完成至少 1 个完整睡眠周期 (约 7-8 小时)

### Q3: STA/LTA 阈值是什么？
- STA/LTA = Short-Time Average / Long-Time Average
- 比值 > 阈值时触发干预
- 默认阈值 1.5

---

## 联系我们

- 🌐 [Somnil 产品官网](https://somnil.top)
- 📧 邮箱：somnil@melspectrum.com
- 🔧 [Piano Tuner 官网](https://pianotuner.top)

**© 2026 融谱智能科技 (深圳) 有限公司 / MelSpectrum Technology (Shenzhen) Co., Ltd.**