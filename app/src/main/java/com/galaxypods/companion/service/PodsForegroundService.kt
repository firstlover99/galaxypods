// 무선 이어폰 상태 추적용 포그라운드 서비스 — connectedDevice 타입
package com.galaxypods.companion.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.galaxypods.companion.GalaxyPodsApp
import com.galaxypods.companion.R
import com.galaxypods.companion.data.location.LastLocationStore
import com.galaxypods.companion.data.preferences.AppPreferences
import com.galaxypods.companion.data.system.MediaController
import com.galaxypods.companion.data.system.ScreenOnReceiver
import com.galaxypods.companion.data.system.VoiceAnnouncer
import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.domain.model.LastLocation
import com.galaxypods.companion.domain.repository.PodsRepository
import com.galaxypods.companion.domain.usecase.AutoPlayPause
import com.galaxypods.companion.domain.usecase.CaseLostDetect
import com.galaxypods.companion.domain.usecase.CaseOpenDetect
import com.galaxypods.companion.presentation.MainActivity
import com.galaxypods.companion.presentation.widget.AppWidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 백그라운드에서 무선 이어폰 상태를 추적하는 FGS.
 *
 * **타입.** Android 14+ 의무인 `connectedDevice`. 본 서비스가 사용자 단말과
 * 연결된 BLE 기기(이어폰) 상태를 추적함을 시스템에 명시.
 *
 * **알림.** 상시 표시. 좌/우/케이스 배터리 % + 모델명 + 미디어 컨트롤 4종
 * (재생/정지/다음/이전/Voice Assistant).
 *
 * **수명.** [startService] 또는 [startForegroundService] 시작 → [stopSelf]/[stopService]
 * 또는 사용자가 알림에서 "종료" 누름.
 */
@AndroidEntryPoint
class PodsForegroundService : Service() {
    @Inject lateinit var repository: PodsRepository

    @Inject lateinit var voiceAnnouncer: VoiceAnnouncer

    @Inject lateinit var mediaController: MediaController

    @Inject lateinit var autoPlayPause: AutoPlayPause

    @Inject lateinit var caseOpenDetect: CaseOpenDetect

    @Inject lateinit var caseOpenNotifier: CaseOpenNotifier

    @Inject lateinit var lastLocationStore: LastLocationStore

    @Inject lateinit var preferences: AppPreferences

    @Inject lateinit var widgetUpdater: AppWidgetUpdater

    @Inject lateinit var caseLostDetect: CaseLostDetect

    @Inject lateinit var caseLostNotifier: CaseLostNotifier

    private val supervisor: Job = SupervisorJob()
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + supervisor)

    private var notificationActionReceiver: NotificationActionReceiver? = null
    private var screenOnReceiver: ScreenOnReceiver? = null
    private var previous: AirPodsAdvertisement? = null
    private var lastConnectionStatus: PodsRepository.ConnectionStatus? = null
    private var pendingLostCheck: kotlinx.coroutines.Job? = null
    private var lastModelName: String = "이어폰"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        android.util.Log.i(TAG, "FGS.onCreate START")
        super.onCreate()
        startInForeground(initialNotification())
        notificationActionReceiver =
            NotificationActionReceiver().also {
                NotificationActionReceiver.register(this, it)
            }
        // 화면 ON 시 스캐너 재시작 (One UI 절전 회복 안전망 — XDA/OpenPods 패턴).
        screenOnReceiver =
            ScreenOnReceiver(onScreenOn = {
                android.util.Log.i(TAG, "SCREEN_ON → repository.startScanning() idempotent")
                runCatching { repository.startScanning() }
            }).also {
                ScreenOnReceiver.register(this, it)
            }
        voiceAnnouncer.initialize()
        observeRepository()
        android.util.Log.i(TAG, "FGS.onCreate END")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        android.util.Log.i(TAG, "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                android.util.Log.i(TAG, "Triggering repository.startScanning()")
                repository.startScanning()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        repository.stopScanning()
        notificationActionReceiver?.let { runCatching { unregisterReceiver(it) } }
        notificationActionReceiver = null
        screenOnReceiver?.let { ScreenOnReceiver.unregister(this, it) }
        screenOnReceiver = null
        voiceAnnouncer.shutdown()
        supervisor.cancel()
        super.onDestroy()
    }

    private fun observeRepository() {
        repository.advertisement
            .onEach { current ->
                if (current != null) {
                    autoPlayPause.onAdvertisement(current)
                    voiceAnnouncer.announceIfNeeded(previous, current)
                    caseOpenDetect.onAdvertisement(current) { ad ->
                        caseOpenNotifier.showCaseOpenAlert(ad)
                    }
                    widgetUpdater.onAdvertisement(current)
                    lastModelName = current.model.displayName
                    previous = current
                }
                updateNotification(current)
            }
            .launchIn(scope)

        repository.connectionStatus
            .onEach { status ->
                handleConnectionTransition(status)
                lastConnectionStatus = status
                updateNotification(repository.advertisement.value)
            }
            .launchIn(scope)
    }

    /** CONNECTED → DISCONNECTED 전이 시 마지막 위치 fetch + 분실 감지 예약 + 자동 일시정지. */
    private fun handleConnectionTransition(newStatus: PodsRepository.ConnectionStatus) {
        val wasConnected = lastConnectionStatus == PodsRepository.ConnectionStatus.CONNECTED
        val nowDisconnected = newStatus == PodsRepository.ConnectionStatus.DISCONNECTED
        val nowConnected = newStatus == PodsRepository.ConnectionStatus.CONNECTED

        // 재연결 시 분실 감지 예약 취소 + 자동 정지된 경우 재생 재개
        if (nowConnected) {
            pendingLostCheck?.cancel()
            pendingLostCheck = null
            if (!wasConnected) {
                runCatching { autoPlayPause.onClassicReconnected() }
            }
            return
        }

        if (!wasConnected || !nowDisconnected) return

        // A2DP 끊김 (케이스에 넣어 닫음) → 자동 일시정지. 신펌웨어 AirPods 비루트에서
        // 유일하게 보장되는 자동 정지 신호 (Type 0x07 광고 의존 X).
        runCatching { autoPlayPause.onClassicDisconnected() }

        scope.launch {
            val locationEnabled = preferences.locationRecordEnabled.firstOrNull() ?: false
            val capturedAtDisconnect: LastLocation? =
                if (locationEnabled) {
                    lastLocationStore.captureNow()
                } else {
                    null
                }

            // 분실 감지 예약 (옵트인 시)
            val lostEnabled = preferences.caseLostAlertEnabled.firstOrNull() ?: false
            if (!lostEnabled || capturedAtDisconnect == null) return@launch

            caseLostDetect.enabled = true
            pendingLostCheck?.cancel()
            pendingLostCheck =
                scope.launch {
                    kotlinx.coroutines.delay(CaseLostDetect.DELAY_MS)
                    runLostCheck(capturedAtDisconnect)
                }
        }
    }

    private suspend fun runLostCheck(disconnectedAt: LastLocation) {
        val stillDisconnected =
            repository.connectionStatus.value !=
                PodsRepository.ConnectionStatus.CONNECTED
        if (!stillDisconnected) return

        val current = lastLocationStore.captureNow() ?: return
        if (caseLostDetect.shouldAlert(disconnectedAt, current, stillDisconnected = true)) {
            val distance =
                caseLostDetect.haversineMeters(
                    disconnectedAt.latitude,
                    disconnectedAt.longitude,
                    current.latitude,
                    current.longitude,
                )
            caseLostNotifier.showLostAlert(distance, lastModelName)
        }
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun initialNotification(): Notification =
        buildNotification(content = getString(R.string.fgs_notification_title))

    private fun updateNotification(ad: AirPodsAdvertisement?) {
        val content =
            when {
                ad == null -> getString(R.string.fgs_notification_title)
                else -> formatBatteryContent(ad)
            }
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(content, ad))
    }

    private fun formatBatteryContent(ad: AirPodsAdvertisement): String {
        val left = ad.leftBatteryPercent.takeIf { it >= 0 }?.let { "L $it%" } ?: "L —"
        val right = ad.rightBatteryPercent.takeIf { it >= 0 }?.let { "R $it%" } ?: "R —"
        val case = ad.caseBatteryPercent.takeIf { it >= 0 }?.let { "📦 $it%" } ?: "📦 —"
        val ts = ad.timestamp.takeIf { it > 0 }?.let { " · ${formatTimeAgoShort(it)}" } ?: ""
        return "${ad.model.displayName}  ·  $left  $right  $case$ts"
    }

    /** 알림용 짧은 상대시간. */
    private fun formatTimeAgoShort(timestamp: Long): String {
        val diffMs = System.currentTimeMillis() - timestamp
        if (diffMs < 0) return "방금"
        val minutes = diffMs / 60_000L
        val hours = minutes / 60
        return when {
            minutes < 1 -> "방금"
            minutes < 60 -> "${minutes}분 전"
            hours < 24 -> "${hours}시간 전"
            else -> "오래 전"
        }
    }

    private fun buildNotification(content: String): Notification = buildNotification(content, ad = null)

    private fun buildNotification(
        content: String,
        ad: AirPodsAdvertisement?,
    ): Notification {
        val openApp =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val stopSelf =
            PendingIntent.getService(
                this,
                REQUEST_STOP,
                Intent(this, PodsForegroundService::class.java).apply { action = ACTION_STOP },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        // 알림바 동적 % 아이콘 (좌/우 중 낮은 쪽 표시) — AndroPods 차용
        val iconPercent =
            ad?.let {
                val l = it.leftBatteryPercent
                val r = it.rightBatteryPercent
                when {
                    l < 0 && r < 0 -> -1
                    l < 0 -> r
                    r < 0 -> l
                    else -> minOf(l, r)
                }
            } ?: -1

        val builder = NotificationCompat.Builder(this, GalaxyPodsApp.CHANNEL_FGS)
        if (ad != null) {
            builder.setSmallIcon(BatteryIcon.createPercentIcon(iconPercent))
        } else {
            builder.setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
        }
        return builder
            .setContentTitle(getString(R.string.fgs_notification_title))
            .setContentText(content)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(
                android.R.drawable.ic_media_previous,
                "이전",
                NotificationActionReceiver.pendingIntent(this, NotificationActionReceiver.ACTION_PREVIOUS),
            )
            .addAction(
                android.R.drawable.ic_media_play,
                "재생/정지",
                NotificationActionReceiver.pendingIntent(this, NotificationActionReceiver.ACTION_PLAY_PAUSE),
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "다음",
                NotificationActionReceiver.pendingIntent(this, NotificationActionReceiver.ACTION_NEXT),
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "종료",
                stopSelf,
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2),
            )
            .build()
    }

    companion object {
        private const val TAG = "GalaxyPods/FGS"
        const val NOTIFICATION_ID: Int = 1001
        const val REQUEST_STOP: Int = 100
        const val ACTION_STOP: String = "com.galaxypods.companion.FGS_STOP"

        fun start(context: Context) {
            val intent = Intent(context, PodsForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PodsForegroundService::class.java))
        }
    }
}
