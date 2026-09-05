package com.orion.app.core

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.orion.app.alerts.DisasterAlertMonitor
import com.orion.app.audio.ClapDetector
import com.orion.app.audio.ClapEvent
import com.orion.app.audio.HotwordListener
import com.orion.app.audio.KnockDetector
import com.orion.app.audio.TextToSpeechManager
import com.orion.app.audio.VoiceInputManager
import com.orion.app.location.LocationHelper
import com.orion.app.network.ChatRepository
import com.orion.app.network.ChatResult
import com.orion.app.network.DisasterApiModule
import com.orion.app.network.NetworkModule
import com.orion.app.network.WeatherApiService
import com.orion.app.network.WeatherModule
import com.orion.app.reminders.ReminderRepository
import com.orion.app.settings.SettingsRepository
import com.orion.app.system.BatteryStats
import com.orion.app.system.CpuStats
import com.orion.app.system.NetworkStats
import com.orion.app.system.RamStats
import com.orion.app.system.StorageStats
import com.orion.app.system.SystemStatsProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class HudState(
    val ram: RamStats? = null,
    val storage: StorageStats? = null,
    val battery: BatteryStats? = null,
    val network: NetworkStats? = null,
    val cpu: CpuStats? = null,
    val latencyMs: Long? = null,
    val backendReachable: Boolean = false
)

class OrionViewModel(
    private val appContext: Context,
    private val stats: SystemStatsProvider,
    private val chatRepository: ChatRepository,
    private val clapDetector: ClapDetector,
    private val voiceInput: VoiceInputManager,
    private val settings: SettingsRepository,
    private val hotwordListener: HotwordListener,
    private val knockDetector: KnockDetector,
    private val tts: TextToSpeechManager,
    private val weatherApi: WeatherApiService,
    private val locationHelper: LocationHelper,
    private val reminderRepository: ReminderRepository,
    private val disasterMonitor: DisasterAlertMonitor
) : ViewModel() {

    private val conversationId = UUID.randomUUID().toString()

    private val _coreState = MutableStateFlow(CoreState.IDLE)
    val coreState: StateFlow<CoreState> = _coreState

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage(ChatMessage.Role.ORION, "Sistemler nominal. Komut bekleniyor."))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _hud = MutableStateFlow(HudState())
    val hud: StateFlow<HudState> = _hud

    private val _isAwake = MutableStateFlow(true)
    val isAwake: StateFlow<Boolean> = _isAwake

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging

    private val _isDndActive = MutableStateFlow(false)
    val isDndActive: StateFlow<Boolean> = _isDndActive

    private val _activeAlertMessage = MutableStateFlow<String?>(null)
    val activeAlertMessage: StateFlow<String?> = _activeAlertMessage

    val isListeningForClaps: StateFlow<Boolean> = clapDetector.isListening

    private var batteryReceiver: BroadcastReceiver? = null

    init {
        pollSystemStats()
        observeClaps()
        registerBatteryReceiver()
        startKnockDetector()
        startDisasterMonitor()
        // NOTE ON MIC CONTENTION: clapDetector (raw AudioRecord) and
        // hotwordListener (SpeechRecognizer) both want the microphone. Many
        // Android devices only allow one active audio-input client at a
        // time, so running double-clap detection AND "Hey Orion" listening
        // simultaneously can cause one of them to fail silently depending
        // on the device. In practice pick ONE as your primary always-on
        // trigger (clap is cheaper on battery); the other still works as a
        // manual fallback when explicitly triggered.
        hotwordListener.start { onWakePhraseHeard() }
    }

    // ---------------------------------------------------------------
    // Wake / sleep / emergency shutdown (feature #1 and #3)
    // ---------------------------------------------------------------

    private fun onWakePhraseHeard() {
        if (!_isAwake.value) wake()
    }

    private fun observeClaps() {
        viewModelScope.launch {
            clapDetector.event.collect { event ->
                when (event) {
                    ClapEvent.DOUBLE_CLAP_DETECTED -> if (_isAwake.value) sleep() else wake()
                    ClapEvent.TRIPLE_CLAP_DETECTED -> emergencyShutdown()
                    else -> {}
                }
            }
        }
    }

    private fun wake() {
        _isAwake.value = true
        _coreState.value = CoreState.LISTENING
        viewModelScope.launch {
            val battery = stats.getBattery()
            val location = locationHelper.getLastKnownLocation()
            val temperature = location?.let {
                try { weatherApi.getCurrent(it.lat, it.lon).current.temperature_2m } catch (e: Exception) { null }
            }
            val reminderCount = try { reminderRepository.todayCount() } catch (e: Exception) { 0 }

            val briefing = BriefingBuilder.build(battery, temperature, reminderCount)
            tts.speak(briefing)
            _messages.update { it + ChatMessage(ChatMessage.Role.ORION, briefing) }
            delay(600)
            _coreState.value = CoreState.IDLE
        }
    }

    private fun sleep() {
        _isAwake.value = false
        _coreState.value = CoreState.IDLE
        voiceInput.stopListening()
    }

    private fun emergencyShutdown() {
        tts.speak("Orion kapatılıyor.")
        _isAwake.value = false
        _coreState.value = CoreState.IDLE
        clapDetector.stop()
        hotwordListener.stop()
        voiceInput.stopListening()
    }

    // ---------------------------------------------------------------
    // Charging state (feature #2) — real-time via a sticky broadcast,
    // not a 5s poll, so the AI Core's lightning effect reacts instantly.
    // ---------------------------------------------------------------

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                _isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        batteryReceiver = receiver
        appContext.registerReceiver(receiver, filter)
    }

    // ---------------------------------------------------------------
    // Do Not Disturb via double-knock (feature #3)
    // ---------------------------------------------------------------

    private fun startKnockDetector() {
        knockDetector.start()
        viewModelScope.launch {
            knockDetector.doubleKnockDetected.collect { detected ->
                if (detected) toggleDnd()
            }
        }
    }

    /**
     * Requires the user to have granted "Do Not Disturb access" once via
     * Settings > Notifications — Android does not allow this as a normal
     * runtime permission dialog. If it's not granted yet, this silently
     * no-ops; wire a prompt in the Settings screen that deep-links to
     * android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS.
     */
    private fun toggleDnd() {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) return
        val turningOn = !_isDndActive.value
        nm.setInterruptionFilter(
            if (turningOn) NotificationManager.INTERRUPTION_FILTER_PRIORITY
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
        _isDndActive.value = turningOn
    }

    // ---------------------------------------------------------------
    // Disaster alert (feature #3) — see DisasterApiService's doc comment
    // for the important accuracy/liability disclaimer.
    // ---------------------------------------------------------------

    private fun startDisasterMonitor() {
        disasterMonitor.start { locationHelper.getLastKnownLocation() }
        viewModelScope.launch {
            disasterMonitor.alert.collect { alert ->
                if (alert != null) {
                    _activeAlertMessage.value = alert.message
                    _coreState.value = CoreState.ALERT
                    tts.speak(alert.message)
                }
            }
        }
    }

    fun acknowledgeAlert() {
        disasterMonitor.clearAlert()
        _activeAlertMessage.value = null
        _coreState.value = CoreState.IDLE
    }

    // ---------------------------------------------------------------
    // Existing chat / voice / HUD polling
    // ---------------------------------------------------------------

    private fun pollSystemStats() {
        viewModelScope.launch {
            while (true) {
                val latency = chatRepository.measureLatencyMs()
                _hud.update {
                    it.copy(
                        ram = stats.getRam(),
                        storage = stats.getStorage(),
                        battery = stats.getBattery(),
                        network = stats.getNetwork(),
                        cpu = stats.getCpu(),
                        latencyMs = latency,
                        backendReachable = latency != null
                    )
                }
                delay(5000)
            }
        }
    }

    fun startClapListening() = clapDetector.start()
    fun stopClapListening() = clapDetector.stop()

    fun activateVoiceInput() {
        _coreState.value = CoreState.LISTENING
        voiceInput.startListening { spokenText ->
            if (spokenText.isNotBlank()) sendMessage(spokenText)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        _messages.update { it + ChatMessage(ChatMessage.Role.USER, text) }
        _coreState.value = CoreState.THINKING

        viewModelScope.launch {
            delay(250)
            _coreState.value = CoreState.SEARCHING
            when (val result = chatRepository.send(text, conversationId)) {
                is ChatResult.Success -> {
                    _coreState.value = CoreState.EXECUTING
                    delay(300)
                    _messages.update { it + ChatMessage(ChatMessage.Role.ORION, result.reply) }
                    tts.speak(result.reply)
                    _coreState.value = CoreState.COMPLETE
                }
                is ChatResult.Failure -> {
                    _messages.update {
                        it + ChatMessage(
                            ChatMessage.Role.ORION,
                            "Backend'e ulaşılamadı: ${result.reason}\n" +
                                "(Settings > Backend URL alanını kontrol edin.)"
                        )
                    }
                    _coreState.value = CoreState.COMPLETE
                }
            }
            delay(1400)
            _coreState.value = CoreState.IDLE
        }
    }

    override fun onCleared() {
        super.onCleared()
        clapDetector.stop()
        voiceInput.stopListening()
        hotwordListener.stop()
        knockDetector.stop()
        disasterMonitor.stop()
        tts.shutdown()
        batteryReceiver?.let { appContext.unregisterReceiver(it) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val settingsRepo = SettingsRepository(appContext)
                val api = NetworkModule.buildApiService(com.orion.app.BuildConfig.DEFAULT_BACKEND_URL)
                return OrionViewModel(
                    appContext = appContext,
                    stats = SystemStatsProvider(appContext),
                    chatRepository = ChatRepository(api),
                    clapDetector = ClapDetector(),
                    voiceInput = VoiceInputManager(appContext),
                    settings = settingsRepo,
                    hotwordListener = HotwordListener(appContext),
                    knockDetector = KnockDetector(appContext),
                    tts = TextToSpeechManager(appContext),
                    weatherApi = WeatherModule.build(),
                    locationHelper = LocationHelper(appContext),
                    reminderRepository = ReminderRepository(appContext),
                    disasterMonitor = DisasterAlertMonitor(DisasterApiModule.build())
                ) as T
            }
        }
    }
}
