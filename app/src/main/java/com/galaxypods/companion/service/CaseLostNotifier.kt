// 케이스 분실 알림 — "AirPods를 두고 가셨나요?" (PodsLink/AndroPods 미보유 차별화)
package com.galaxypods.companion.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.galaxypods.companion.GalaxyPodsApp
import com.galaxypods.companion.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 케이스 분실 가능성 알림.
 *
 * **차용 출처.** competitive-analysis §5 S급 4번 — 적극적 분실 방지.
 *
 * `case_open_alert` 채널 재사용 (HIGH 중요도). 사용자는 알림 탭으로 메인 화면
 * 진입해 마지막 위치 확인 가능.
 */
@Singleton
class CaseLostNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun showLostAlert(
            distanceMeters: Double,
            modelName: String,
        ) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return

            val openApp =
                PendingIntent.getActivity(
                    context,
                    REQUEST_OPEN,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val notification =
                NotificationCompat.Builder(context, GalaxyPodsApp.CHANNEL_CASE_OPEN)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle("$modelName 두고 가셨나요?")
                    .setContentText("연결이 끊긴 위치에서 약 ${distanceMeters.toInt()}m 떨어져 있습니다. 탭해서 마지막 위치를 확인하세요.")
                    .setStyle(
                        NotificationCompat.BigTextStyle().bigText(
                            "이어폰 연결이 끊긴 후 ${distanceMeters.toInt()}m 멀어졌습니다.\n\n" +
                                "메인 화면에서 \"마지막 위치 보기\"를 눌러 정확한 지점을 확인할 수 있습니다.",
                        ),
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setContentIntent(openApp)
                    .build()

            nm.notify(NOTIFICATION_ID, notification)
        }

        companion object {
            const val NOTIFICATION_ID: Int = 2002
            const val REQUEST_OPEN: Int = 201
        }
    }
