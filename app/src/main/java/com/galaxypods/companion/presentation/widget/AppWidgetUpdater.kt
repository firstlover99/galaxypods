// 위젯 갱신 트리거 — FGS가 광고 받을 때마다 호출
package com.galaxypods.companion.presentation.widget

import android.content.Context
import com.galaxypods.companion.data.preferences.AppPreferences
import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.domain.model.WidgetSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 위젯 즉시 갱신 + DataStore 영속화 통합.
 *
 * **사용처.** [com.galaxypods.companion.service.PodsForegroundService]가 광고 콜백에서
 * 호출. 영속화된 스냅샷은 다음 위젯 onUpdate에서도 사용 (Activity 재시작 후에도 표시).
 */
@Singleton
class AppWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AppPreferences,
) {
    /**
     * 새 광고 → 위젯 갱신 + DataStore 영속화.
     *
     * @return 영속화에 성공했는지 (호출자가 await 필요 시).
     */
    suspend fun onAdvertisement(ad: AirPodsAdvertisement) {
        val snapshot = WidgetSnapshot.fromAdvertisement(ad)
        preferences.setWidgetSnapshot(snapshot)
        PodsAppWidgetProvider.pushUpdate(context, snapshot)
    }
}
