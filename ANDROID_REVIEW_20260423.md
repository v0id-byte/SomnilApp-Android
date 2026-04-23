# Somnil Android App — Code Review Report

**审查日期：** 2026-04-23  
**审查人：** Melt (Subagent)  
**仓库：** `v0id-byte/SomnilApp-Android`（分支：main，尚未有 commits）  
**审查范围：** UI/UX、逻辑/业务、算法调用、API、BLE 通信、与 iOS 对齐

---

## 发现问题汇总（共 16 个）

| 严重程度 | 数量 |
|----------|------|
| **P0**（阻断性Bug） | 3 |
| **P1**（功能缺失/错误） | 7 |
| **P2**（代码质量/UI） | 6 |

---

## P0 — 阻断性 Bug

### P0-1：SomnilPacket.fromByteArray 字节序错误
**文件：** `app/src/main/java/com/somnil/app/domain/model/SomnilModels.kt:35-36`  
**描述：** 解析 8 通道 16-bit signed big-endian 数据时，`high` 和 `low` 位置写反了。实际读取的是 little-endian，导致所有通道数据都是字节交换后的错误值，直接影响 FFT 特征提取和 STA/LTA 计算的输入数据正确性。

```kotlin
// 当前代码（错误）：
val low = data[i * 2 + 1].toUShort()
val high = data[i * 2 + 2].toUShort()
channelData.add(((high shl 8) or low).toShort())

// 应为（big-endian 高位在前）：
val low = data[i * 2 + 1].toUShort()   // 低字节
val high = data[i * 2 + 2].toUShort()  // 高字节
channelData.add(((high shl 8) or low).toShort())  // ✅ 顺序本身是对的，但上面变量名写反了（low/high 标签互换）
```

**影响：** EEG 数据流的最上游就是错的，后续所有算法输出（STA/LTA、焦虑检测、睡眠分期）全部受影响。

---

### P0-2：训练期推进逻辑存在缺陷
**文件：** `app/src/main/java/com/somnil/app/service/TrainingDataStore.kt:151-153`  
**描述：** `recordSession()` 中的推进条件逻辑不清，且未检查是否同一天重复记录。当前逻辑：

```kotlin
val daysPassed = getDaysPassed(phase.startDate)
if (daysPassed >= phase.completedDays && phase.successfulAdaptations >= phase.completedDays + 1) {
    phase = phase.copy(completedDays = minOf(3, phase.completedDays + 1))
}
```

问题：
- `daysPassed >= phase.completedDays` 在第 0 天时 `0 >= 0` 即为 true，可提前推进
- 不检查当天是否已记录过，导致刷新页面可重复累加 `successfulAdaptations`
- iOS 版本对应逻辑是否一致？需交叉验证

---

### P0-3：无 REST API 集成（仅有 MQTT）
**文件：** 整个项目  
**描述：** 项目中**没有任何 HTTP/REST API 调用**。`SettingsViewModel.connectMQTT()` 调用的 `mqttManager.connect()` 方法体为空（只有 `disconnect()` + `return`），MQTT 连接从未真正建立。

根据需求应有以下 API 端点，但均未实现：
- `POST /api/auth/login` — 用户认证
- `POST /api/sessions/upload` — 睡眠数据上传
- `GET /api/models/{modelId}` — ML 模型获取
- `GET /api/users/profile` — 用户配置获取

**影响：** App 无法与后端通信，所有云端功能（用户体系、数据同步、模型更新）不可用。

---

## P1 — 功能缺失 / 逻辑错误

### P1-1：MQTT connect 参数被忽略
**文件：** `app/src/main/java/com/somnil/app/service/MQTTManager.kt:44-46`  
**描述：** `connect()` 方法接收 `config` 参数后，直接 `disconnect()` 然后 `return`，未使用 `config` 中的 `brokerHost`/`brokerPort`。Settings UI 中用户输入的 HA 地址端口完全无效。

---

### P1-2：FFT 实现为空（仅有 sum-of-squares 代理）
**文件：** `app/src/main/java/com/somnil/app/service/DataProcessor.kt:172-182`  
**描述：** `calculateBetaPower`、`calculateVLFPower`、`calculateEMGPower` 均用简化的 `take(n).map {it * it}` 代替 FFT，代码注释自己也写了 `"Real implementation would use FFT"`。这导致频段能量估算不准确，与 iOS 版使用 vDSP FFT 的实现严重偏离。

---

### P1-3：Foreground Service 从未启动
**文件：** `app/src/main/java/com/somnil/app/service/BLEMonitoringService.kt` + `MainActivity.kt`  
**描述：** `BLEMonitoringService` 已声明并实现了 `onStartCommand`，但项目中**没有任何代码调用 `startService()`**。后台监测保活机制形同虚设，用户将 App 切到后台时 BLE 连接会被系统切断。

---

### P1-4：HistoryViewModel 始终返回空列表
**文件：** `app/src/main/java/com/somnil/app/ui/screens/history/HistoryViewModel.kt:26`  
**描述：** `loadSessions()` 内部只有一个 `_sessions.value = emptyList()`，TODO 注释说要接 Room 数据库但未实现。历史记录功能完全不可用。

---

### P1-5：BLE 扫描结果缺少_LOCATION 权限过滤
**文件：** `app/src/main/java/com/somnil/app/service/BLEManager.kt:168-175`  
**描述：** 虽然已有 `BLUETOOTH_CONNECT` 权限检查，但 Android 11（API 30+）上 BLE scan results **需要 `ACCESS_FINE_LOCATION` 权限**才能获取设备名称，否则 `device.name` 和 `result.scanRecord?.deviceName` 可能为 null。虽然 Manifest 中声明了 `ACCESS_FINE_LOCATION`，但未做运行时权限申请和检查（iOS 版无需此权限，是 Android BLE 的平台限制）。

---

### P1-6：TrainingViewModel.recordSession 训练天数逻辑缺陷
**文件：** `app/src/main/java/com/somnil/app/ui/screens/training/TrainingViewModel.kt:51-70`  
**描述：** `TrainingViewModel.recordSession()` 调用链：ViewModel → `TrainingDataStore.recordNightData()` → `TrainingDataStore.recordSession()`。但训练数据（avgSTALTA/stdSTALTA）只在 `recordNightData()` 中记录到 SharedPreferences，而训练阶段推进逻辑 `recordSession()` 并未实际使用这些数据来计算阈值，仅检查 `successfulAdaptations` 计数。**iOS 版的个性化阈值计算**（Railsback curve / basinhopping）在 Android 版完全缺失。

---

### P1-7：BLE 自动重连在断开后不会重试
**文件：** `app/src/main/java/com/somnil/app/service/BLEManager.kt:248-259`  
**描述：** `startAutoReconnect()` 只在 `onConnectionStateChange` 的 `status != 0`（异常断开）时触发，且只尝试一次后就开始扫描。`autoReconnectJob` 中没有循环逻辑，无法实现持续重连。用户意外断开后 App 不会自动恢复连接。

---

## P2 — 代码质量 / UI

### P2-1：UI 硬编码中文字符串
**文件：** 所有 `*.kt` View 文件（如 `TrainingView.kt`、`HomeView.kt`、`SettingsView.kt`）  
**描述：** 几乎所有屏幕文本都是直接写死的中文字符串（如 `"第 $day 天"`、`"监测中"`、`"已完成"`），未使用 `strings.xml` 资源文件。App 无法国际化，且与 iOS 的 `String Catalogs` 体系不对齐。

---

### P2-2：SomnilComponents.SleepStageBar getStageColor 重复实现
**文件：** `app/src/main/java/com/somnil/app/ui/components/SomnilComponents.kt:279-286` 与 `HistoryView.kt:351-358`、`LiveMonitorView.kt:469-476`  
**描述：** `getStageColor()` 在 3 个文件中重复定义了相同的 when 分支，而非统一到 `SomnilEnums.kt` 的 `SleepStage.colorHex` 属性或 Theme 文件中。

---

### P2-3：SettingsViewModel 中 MQTT connect 未传参
**文件：** `app/src/main/java/com/somnil/app/ui/screens/settings/SettingsViewModel.kt:77`  
**描述：** `connectMQTT()` 的实现在 `SettingsViewModel` 中直接调用 `mqttManager.connect()`，未将用户输入的 `host`、`port` 参数传递给 manager（且 manager.connect 本身也是空的）。

---

### P2-4：AudioPlayerManager 中 `sin`/`cos` 函数使用 double 版
**文件：** `app/src/main/java/com/somnil/app/service/AudioPlayerManager.kt:125-134`  
**描述：** 代码使用 `sin(2 * Math.PI * ...)`，`Math.PI` 是 `Double`，混用可能会在某些 Kotlin 版本中导致精度问题。建议使用 `kotlin.math.sin` / `kotlin.math.cos`。

---

### P2-5：BLEManager.onReadRemoteRssi 状态更新覆盖完整 Connected 状态
**文件：** `app/src/main/java/com/somnil/app/service/BLEManager.kt:151-155`  
**描述：** 每次 RSSI 更新时重建 `ConnectionState.Connected` 对象会丢失 `rssi` 以外的其他状态信息（如 Peripheral 引用）。虽然当前只有 RSSI 在变，但设计不良。

---

### P2-6：DataProcessor scope 未在 onDestroy 中取消
**文件：** `app/src/main/java/com/somnil/app/service/DataProcessor.kt:10`  
**描述：** `DataProcessor` 持有 `CoroutineScope`，但在 `onDestroy`（或等价生命周期）时未调用 `scope.cancel()`，会导致协程泄漏。

---

## 与 iOS 版本对齐问题

| 对齐项 | iOS | Android | 状态 |
|--------|-----|---------|------|
| FFT 实现 | vDSP (8 通道频谱) | sum-of-squares 代理 | ❌ 未对齐 |
| 训练期个性化阈值 | Basinhopping 后端计算 Railsback curve | 未实现 | ❌ 未对齐 |
| REST API | Alamofire 完整调用 | 零 HTTP 调用 | ❌ 未对齐 |
| MQTT | 连接 + 发布 + 订阅完整 | 仅有骨架 | ⚠️ 部分对齐 |
| History 持久化 | Core Data | 未实现（空列表） | ❌ 未对齐 |
| Foreground Service | BackgroundModes | 未启动 | ⚠️ 部分对齐 |
| 训练 Tab | 完整实现 | 完整实现 | ✅ 已对齐 |
| BLE Nordic UART | 完整实现 | 完整实现 | ✅ 已对齐 |

---

## 优先修复建议

1. **立即修复 P0-1**（字节序）— 上游数据源头，必须先修
2. **立即补充 REST API 集成**（P0-3）— App 核心价值依赖
3. **修复训练期逻辑**（P0-2）— 防止用户刷数据绕过训练期
4. **启动 Foreground Service**（P1-3）— 后台监测必现 bug
5. **实现 FFT**（P1-2）— 算法精度核心，当前是占位符
6. **MQTT connect 修复**（P1-1）— HA 集成无法工作

---

*本报告由 Melt Subagent 生成，审查深度：全量源码扫描 + 交叉文件对比。*
