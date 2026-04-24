package com.somnil.app.data.model

/**
 * Repository of all smart home ecosystem guides.
 * All content is hardcoded — no server calls.
 */
object SmartHomeGuideRepository {

    val allEcosystems: List<EcosystemGuide> = listOf(
        homeAssistantGuide,
        appleHomeGuide,
        xiaomiGuide,
        manualMqttGuide
    )

    // ─── Home Assistant (5 steps) ────────────────────────────────────────────

    private val homeAssistantGuide = EcosystemGuide(
        id = "home_assistant",
        name = "Home Assistant",
        description = "开源自托管，最灵活",
        iconEmoji = "🏠",
        priority = EcosystemPriority.HIGH,
        steps = listOf(
            GuideStep(
                stepNumber = 1,
                totalSteps = 5,
                title = "打开 Home Assistant",
                instruction = "在浏览器地址栏输入你的 Home Assistant 地址，例如：\n\nhttp://192.168.1.100:8123\n\n如果你不知道地址，可以在 Home Assistant 的「设置」→「系统」→「网络」中查看。",
                screenshotHint = "浏览器打开 HA 界面"
            ),
            GuideStep(
                stepNumber = 2,
                totalSteps = 5,
                title = "进入「配置」菜单",
                instruction = "在左侧边栏找到并点击「配置」（Settings）\n\n然后在子菜单中点击「设备与服务」（Devices & Services）",
                screenshotHint = "HA 侧边栏 → 配置 → 设备与服务"
            ),
            GuideStep(
                stepNumber = 3,
                totalSteps = 5,
                title = "添加集成",
                instruction = "在「设备与服务」页面，点击右下角的蓝色按钮：\n\n「+ 添加集成」（Add Integration）\n\n这是添加新设备/服务的第一步。",
                screenshotHint = "右下角 + 添加集成"
            ),
            GuideStep(
                stepNumber = 4,
                totalSteps = 5,
                title = "搜索 MQTT",
                instruction = "在搜索框中输入「MQTT」\n\n在搜索结果中点击「MQTT」集成（通常显示为 Mosquitto broker 或 MQTT）",
                screenshotHint = "搜索框输入 MQTT"
            ),
            GuideStep(
                stepNumber = 5,
                totalSteps = 5,
                title = "填写 MQTT 信息",
                instruction = "从 Somnil App 的「设置」页面复制 MQTT 配置信息，填入以下字段：\n\n• Broker（Broker Host）：MQTT 服务器地址\n• Port：1883\n• Username / Password：如已配置\n\n点击「提交」完成配置。",
                screenshotHint = "MQTT 配置表单"
            )
        )
    )

    // ─── Apple Home (3 steps) ───────────────────────────────────────────────

    private val appleHomeGuide = EcosystemGuide(
        id = "apple_home",
        name = "Apple Home",
        description = "Apple 官方，家庭 App",
        iconEmoji = "🍎",
        priority = EcosystemPriority.MEDIUM,
        steps = listOf(
            GuideStep(
                stepNumber = 1,
                totalSteps = 3,
                title = "打开「家庭」App",
                instruction = "在你的 iPhone 或 iPad 上找到并打开「家庭」App（Home App）。\n\n如果你没有找到，可以在 App 库中搜索「家庭」或「Home」。",
                screenshotHint = "iPhone 桌面 → 家庭 App"
            ),
            GuideStep(
                stepNumber = 2,
                totalSteps = 3,
                title = "添加配件",
                instruction = "在家庭 App 中，点击左上角的「⊕」（家」按钮\n\n然后选择「添加配件」\n\nApp 会开始搜索附近的配件设备。",
                screenshotHint = "家庭 App → ⊕ → 添加配件"
            ),
            GuideStep(
                stepNumber = 3,
                totalSteps = 3,
                title = "配对配件",
                instruction = "当 Somnil 设备出现在列表中时，点击它进行配对。\n\n如果需要输入配对码，请在 Somnil App 设置页查看设备配对码（通常为 000-00-000 格式）。\n\n配对成功后，设备会出现在「房间」中。",
                screenshotHint = "输入配对码界面"
            )
        )
    )

    // ─── 小米米家 (3 steps) ───────────────────────────────────────────────

    private val xiaomiGuide = EcosystemGuide(
        id = "xiaomi_mijia",
        name = "小米米家",
        description = "国内用户量大",
        iconEmoji = "📱",
        priority = EcosystemPriority.MEDIUM,
        steps = listOf(
            GuideStep(
                stepNumber = 1,
                totalSteps = 3,
                title = "打开米家 App",
                instruction = "在手机上打开「米家」App。\n\n如果没有安装，请到应用商店搜索「米家」并安装。",
                screenshotHint = "米家 App 桌面图标"
            ),
            GuideStep(
                stepNumber = 2,
                totalSteps = 3,
                title = "添加设备",
                instruction = "点击右上角的「⊕」按钮\n\n选择「添加设备」或「扫一扫」\n\n如果使用扫一扫，用摄像头扫描 Somnil 设备上的二维码。",
                screenshotHint = "米家 → ⊕ → 添加设备"
            ),
            GuideStep(
                stepNumber = 3,
                totalSteps = 3,
                title = "完成配对",
                instruction = "按照屏幕上的指引完成设备配对。\n\n如果 App 提示输入配对码，请在 Somnil App 设置页查看。\n\n配对成功后，设备会出现在「我的设备」列表中。",
                screenshotHint = "配对成功界面"
            )
        )
    )

    // ─── MQTT 手动配置 (2 steps) ─────────────────────────────────────────

    private val manualMqttGuide = EcosystemGuide(
        id = "mqtt_manual",
        name = "MQTT 手动配置",
        description = "Webhook / 手动 MQTT",
        iconEmoji = "🌐",
        priority = EcosystemPriority.HIGH,
        steps = listOf(
            GuideStep(
                stepNumber = 1,
                totalSteps = 2,
                title = "复制 MQTT 配置",
                instruction = "打开 Somnil App → 设置页面，找到 MQTT 配置区域。\n\n点击下方的「复制配置」按钮，将完整的 MQTT 连接信息复制到剪贴板。\n\n复制的内容包含：服务器地址、端口、用户名（可选）、密码（可选）。",
                screenshotHint = "Somnil 设置 → MQTT 配置区域",
                copyableText = "broker: mqtt.somnil.com\nport: 1883\nusername: your_username\npassword: your_password"
            ),
            GuideStep(
                stepNumber = 2,
                totalSteps = 2,
                title = "填入你的系统",
                instruction = "将复制的配置信息填入你的 MQTT 客户端或自动化平台。\n\n常用配置示例：\n\n• Broker: mqtt.somnil.com\n• Port: 1883\n• Protocol: MQTT v3.1.1\n• TLS: 关闭（默认）\n\nTopic 路径可在 Somnil App 设置页查看。",
                screenshotHint = "MQTT.fx / Home Assistant MQTT 配置"
            )
        )
    )
}
