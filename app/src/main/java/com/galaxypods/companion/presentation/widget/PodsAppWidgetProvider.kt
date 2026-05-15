// 홈 화면 위젯 — 좌/우/케이스 배터리 표시
package com.galaxypods.companion.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.galaxypods.companion.R
import com.galaxypods.companion.data.preferences.AppPreferences
import com.galaxypods.companion.domain.model.WidgetSnapshot
import com.galaxypods.companion.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 홈 화면 위젯 — AirPods/Beats 배터리 3개 (좌/우/케이스).
 *
 * **갱신 트리거.**
 * 1. 시스템 자동 (`appwidget-provider` updatePeriodMillis — 본 앱은 1800000ms = 30분)
 * 2. FGS가 광고 받을 때마다 [PodsAppWidgetUpdater]로 명시 갱신 (즉시 반영)
 * 3. 사용자가 위젯 추가 시 onUpdate
 *
 * 데이터 소스. [AppPreferences.widgetSnapshot] (영속화된 마지막 광고).
 */
@AndroidEntryPoint
class PodsAppWidgetProvider : AppWidgetProvider() {
    @Inject lateinit var preferences: AppPreferences

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // DataStore는 suspend → 별도 scope에서 fetch 후 갱신
        CoroutineScope(Dispatchers.Default).launch {
            val snapshot = preferences.widgetSnapshot.firstOrNull()
            appWidgetIds.forEach { id ->
                val views = buildRemoteViews(context, snapshot)
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }

    companion object {
        /**
         * 외부(FGS / Updater)에서 강제 갱신할 때 호출.
         * suspend가 아니므로 호출 측이 미리 snapshot을 준비.
         */
        fun pushUpdate(
            context: Context,
            snapshot: WidgetSnapshot?,
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, PodsAppWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            ids.forEach { id ->
                manager.updateAppWidget(id, buildRemoteViews(context, snapshot))
            }
        }

        private fun buildRemoteViews(
            context: Context,
            snapshot: WidgetSnapshot?,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_pods_battery)

            views.setTextViewText(
                R.id.widget_model,
                snapshot?.model?.displayName ?: "이어폰 검색 중",
            )
            views.setTextViewText(R.id.widget_left_value, formatPercent(snapshot?.leftBatteryPercent))
            views.setTextViewText(R.id.widget_right_value, formatPercent(snapshot?.rightBatteryPercent))
            views.setTextViewText(R.id.widget_case_value, formatPercent(snapshot?.caseBatteryPercent))

            // 위젯 클릭 → MainActivity 열기
            val openApp =
                PendingIntent.getActivity(
                    context,
                    REQUEST_OPEN_APP,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)

            return views
        }

        private fun formatPercent(percent: Int?): String = percent?.takeIf { it >= 0 }?.let { "$it%" } ?: "—"

        private const val REQUEST_OPEN_APP: Int = 300
    }
}
