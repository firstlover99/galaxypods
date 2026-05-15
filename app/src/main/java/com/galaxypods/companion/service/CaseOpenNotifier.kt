// 케이스 오픈 풀스크린 알림 빌더 — SYSTEM_ALERT_WINDOW 대체 (v1.0)
package com.galaxypods.companion.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.galaxypods.companion.GalaxyPodsApp
import com.galaxypods.companion.R
import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 케이스 오픈 시 표시할 풀스크린 알림.
 *
 * **v1.0 결정 (CLAUDE.md 원칙 12).** SYSTEM_ALERT_WINDOW 오버레이 대신 풀스크린
 * 알림으로 1차 출시. Play 정책 리스크 회피.
 *
 * Android 14+ 풀스크린 인텐트는 사용자 명시 부여 권한 필요. 미부여 시 일반
 * heads-up 알림으로 자동 폴백 (시스템 처리).
 */
@Singleton
class CaseOpenNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun showCaseOpenAlert(ad: AirPodsAdvertisement) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        val openApp = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, GalaxyPodsApp.CHANNEL_CASE_OPEN)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(ad.model.displayName)
            .setContentText(formatBatteryShort(ad))
            .setStyle(NotificationCompat.BigTextStyle().bigText(formatBatteryDetailed(ad)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(AUTO_DISMISS_MS)
            .setContentIntent(openApp)
            .setFullScreenIntent(openApp, /* highPriority = */ true)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun formatBatteryShort(ad: AirPodsAdvertisement): String {
        val l = ad.leftBatteryPercent.takeIf { it >= 0 }?.let { "L $it%" } ?: "L —"
        val r = ad.rightBatteryPercent.takeIf { it >= 0 }?.let { "R $it%" } ?: "R —"
        val c = ad.caseBatteryPercent.takeIf { it >= 0 }?.let { "📦 $it%" } ?: "📦 —"
        return "$l   $r   $c"
    }

    private fun formatBatteryDetailed(ad: AirPodsAdvertisement): String = buildString {
        appendLine(ad.model.displayName)
        appendLine()
        appendLine("왼쪽. ${formatPercent(ad.leftBatteryPercent)}")
        appendLine("오른쪽. ${formatPercent(ad.rightBatteryPercent)}")
        append("케이스. ${formatPercent(ad.caseBatteryPercent)}")
        if (ad.caseCharging) append(" (충전 중)")
    }

    private fun formatPercent(percent: Int): String =
        if (percent < 0) "—" else "$percent%"

    companion object {
        const val NOTIFICATION_ID: Int = 2001
        const val REQUEST_OPEN: Int = 200
        const val AUTO_DISMISS_MS: Long = 5_000L
    }
}
