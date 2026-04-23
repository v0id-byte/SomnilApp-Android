package com.somnil.app.service

import com.somnil.app.domain.model.MQTTConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MQTT Manager for Home Assistant integration - mirrors iOS MQTTManager.
 * Receives "asleep"/"awake" commands from HA to control audio playback.
 */
@Singleton
class MQTTManager @Inject constructor() {
    private var client: MqttClient? = null
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _sleepState = MutableSharedFlow<String>(replay = 1)
    val sleepState: SharedFlow<String> = _sleepState.asSharedFlow()

    private var config = MQTTConfig()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Update MQTT configuration - mirrors iOS mqttManager.updateConfig().
     */
    fun updateConfig(
        host: String,
        port: Int,
        username: String,
        password: String,
        topic: String
    ) {
        config = MQTTConfig(host, port, username, password, topic)
    }

    fun connect() {
        disconnect() // Clean up existing connection

        val brokerUrl = "tcp://${config.brokerHost}:${config.brokerPort}"
        val clientId = "SomnilApp_${UUID.randomUUID().toString().take(8)}"

        try {
            client = MqttClient(brokerUrl, clientId, MemoryPersistence()).apply {
                setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        _isConnected.value = false
                        _errorMessage.value = cause?.message
                    }

                    override fun messageArrived(topic: String, message: MqttMessage) {
                        val payload = String(message.payload)
                        scope.launch {
                            _sleepState.emit(payload.trim())
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                if (config.username.isNotEmpty()) {
                    connect(MqttConnectOptions().apply {
                        userName = config.username
                        password = config.password.toCharArray()
                        isAutomaticReconnect = true
                        isCleanSession = true
                    })
                } else {
                    connect(MqttConnectOptions().apply {
                        isAutomaticReconnect = true
                        isCleanSession = true
                    })
                }

                subscribe(config.sleepStateTopic, 1)
                _isConnected.value = true
                _errorMessage.value = null
            }
        } catch (e: Exception) {
            _errorMessage.value = e.message
            _isConnected.value = false
        }
    }

    fun disconnect() {
        try {
            client?.disconnect()
            client?.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
        client = null
        _isConnected.value = false
    }
}
