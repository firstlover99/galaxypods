// 앱 설정값 영속화 — DataStore Preferences 기반
package com.galaxypods.companion.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.galaxypods.companion.data.system.VoiceAnnouncer
import com.galaxypods.companion.domain.model.AirPodsModel
import com.galaxypods.companion.domain.model.LastLocation
import com.galaxypods.companion.domain.model.WidgetSnapshot
import com.galaxypods.companion.domain.usecase.AutoPlayPause
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "galaxypods_prefs")

/**
 * 앱 전체 설정 영속화.
 *
 * **포함 항목.**
 * - 온보딩 완료 여부 (첫 실행 분기에 사용)
 * - 귀감지 자동 정지 ON/OFF + 모드 (RELAXED_EITHER / STRICT_BOTH)
 * - 음성 안내 ON/OFF + 임계값
 * - 마지막 위치 기능 ON/OFF
 *
 * 모든 값은 Flow로 노출. ViewModel에서 collect 또는 stateIn.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_ONBOARDING_COMPLETED] ?: false }

    val autoPauseEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_AUTO_PAUSE_ENABLED] ?: true }

    val autoPauseMode: Flow<AutoPlayPause.Mode> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[KEY_AUTO_PAUSE_MODE] ?: AutoPlayPause.Mode.RELAXED_EITHER.ordinal
            AutoPlayPause.Mode.values().getOrNull(raw) ?: AutoPlayPause.Mode.RELAXED_EITHER
        }

    val voiceAnnouncerEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_VOICE_ENABLED] ?: false }

    val voiceAnnouncerThreshold: Flow<Int> = context.dataStore.data
        .map { it[KEY_VOICE_THRESHOLD] ?: VoiceAnnouncer.DEFAULT_THRESHOLD_PERCENT }

    val locationRecordEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_LOCATION_RECORD_ENABLED] ?: false }

    val caseLostAlertEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_CASE_LOST_ENABLED] ?: false }

    /** Crashlytics 데이터 수집 동의. **기본 false (옵트인).** CLAUDE.md 원칙 11. */
    val crashlyticsOptIn: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_CRASHLYTICS_OPT_IN] ?: false }

    val widgetSnapshot: Flow<WidgetSnapshot?> = context.dataStore.data.map { prefs ->
        val modelName = prefs[KEY_WIDGET_MODEL] ?: return@map null
        val ts = prefs[KEY_WIDGET_TS] ?: return@map null
        WidgetSnapshot(
            model = runCatching { AirPodsModel.valueOf(modelName) }.getOrDefault(AirPodsModel.UNKNOWN),
            leftBatteryPercent = prefs[KEY_WIDGET_LEFT] ?: -1,
            rightBatteryPercent = prefs[KEY_WIDGET_RIGHT] ?: -1,
            caseBatteryPercent = prefs[KEY_WIDGET_CASE] ?: -1,
            timestamp = ts,
        )
    }

    val lastLocation: Flow<LastLocation?> = context.dataStore.data.map { prefs ->
        val lat = prefs[KEY_LAST_LAT]
        val lng = prefs[KEY_LAST_LNG]
        val ts = prefs[KEY_LAST_TS]
        if (lat != null && lng != null && ts != null) {
            LastLocation(
                latitude = lat,
                longitude = lng,
                timestamp = ts,
                accuracyMeters = prefs[KEY_LAST_ACC],
            )
        } else null
    }

    suspend fun setOnboardingCompleted(value: Boolean) =
        update(KEY_ONBOARDING_COMPLETED, value)

    suspend fun setAutoPauseEnabled(value: Boolean) =
        update(KEY_AUTO_PAUSE_ENABLED, value)

    suspend fun setAutoPauseMode(mode: AutoPlayPause.Mode) =
        update(KEY_AUTO_PAUSE_MODE, mode.ordinal)

    suspend fun setVoiceAnnouncerEnabled(value: Boolean) =
        update(KEY_VOICE_ENABLED, value)

    suspend fun setVoiceAnnouncerThreshold(value: Int) =
        update(KEY_VOICE_THRESHOLD, value)

    suspend fun setLocationRecordEnabled(value: Boolean) =
        update(KEY_LOCATION_RECORD_ENABLED, value)

    suspend fun setCaseLostAlertEnabled(value: Boolean) =
        update(KEY_CASE_LOST_ENABLED, value)

    suspend fun setCrashlyticsOptIn(value: Boolean) =
        update(KEY_CRASHLYTICS_OPT_IN, value)

    suspend fun setLastLocation(location: LastLocation) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_LAT] = location.latitude
            prefs[KEY_LAST_LNG] = location.longitude
            prefs[KEY_LAST_TS] = location.timestamp
            location.accuracyMeters?.let { prefs[KEY_LAST_ACC] = it }
        }
    }

    suspend fun setWidgetSnapshot(snapshot: WidgetSnapshot) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WIDGET_MODEL] = snapshot.model.name
            prefs[KEY_WIDGET_LEFT] = snapshot.leftBatteryPercent
            prefs[KEY_WIDGET_RIGHT] = snapshot.rightBatteryPercent
            prefs[KEY_WIDGET_CASE] = snapshot.caseBatteryPercent
            prefs[KEY_WIDGET_TS] = snapshot.timestamp
        }
    }

    suspend fun clearLastLocation() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_LAST_LAT)
            prefs.remove(KEY_LAST_LNG)
            prefs.remove(KEY_LAST_TS)
            prefs.remove(KEY_LAST_ACC)
        }
    }

    private suspend fun <T> update(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_AUTO_PAUSE_ENABLED = booleanPreferencesKey("auto_pause_enabled")
        private val KEY_AUTO_PAUSE_MODE = intPreferencesKey("auto_pause_mode")
        private val KEY_VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        private val KEY_VOICE_THRESHOLD = intPreferencesKey("voice_threshold")
        private val KEY_LOCATION_RECORD_ENABLED = booleanPreferencesKey("location_record_enabled")
        private val KEY_LAST_LAT = doublePreferencesKey("last_location_lat")
        private val KEY_LAST_LNG = doublePreferencesKey("last_location_lng")
        private val KEY_LAST_TS = longPreferencesKey("last_location_ts")
        private val KEY_LAST_ACC = floatPreferencesKey("last_location_acc")
        private val KEY_WIDGET_MODEL = stringPreferencesKey("widget_model_name")
        private val KEY_WIDGET_LEFT = intPreferencesKey("widget_left")
        private val KEY_WIDGET_RIGHT = intPreferencesKey("widget_right")
        private val KEY_WIDGET_CASE = intPreferencesKey("widget_case")
        private val KEY_WIDGET_TS = longPreferencesKey("widget_ts")
        private val KEY_CASE_LOST_ENABLED = booleanPreferencesKey("case_lost_alert_enabled")
        private val KEY_CRASHLYTICS_OPT_IN = booleanPreferencesKey("crashlytics_opt_in")
    }
}
