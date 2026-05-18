// GalaxyPods Application 진입점 — Hilt 컨테이너 초기화 및 알림 채널 생성
package com.galaxypods.companion

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GalaxyPodsApp : Application() {
    override fun onCreate() {
        Log.i(TAG, "Application.onCreate START")
        super.onCreate()
        runCatching { createNotificationChannels() }
            .onFailure { Log.e(TAG, "createNotificationChannels failed", it) }
        Log.i(TAG, "Application.onCreate END")
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm =
            getSystemService(NotificationManager::class.java) ?: run {
                Log.w(TAG, "NotificationManager is null — skipping channel creation")
                return
            }

        val fgsChannel =
            NotificationChannel(
                CHANNEL_FGS,
                getString(R.string.fgs_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.fgs_channel_desc)
                setShowBadge(false)
            }
        nm.createNotificationChannel(fgsChannel)

        // 케이스 오픈 풀스크린 알림 (v1.0 SYSTEM_ALERT_WINDOW 대체)
        val caseChannel =
            NotificationChannel(
                CHANNEL_CASE_OPEN,
                getString(R.string.case_open_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.case_open_channel_desc)
                setShowBadge(false)
                enableVibration(true)
                enableLights(true)
            }
        nm.createNotificationChannel(caseChannel)
    }

    companion object {
        const val CHANNEL_FGS = "fgs_pods_connection"
        const val CHANNEL_CASE_OPEN = "case_open_alert"
        private const val TAG = "GalaxyPods/App"
    }
}
