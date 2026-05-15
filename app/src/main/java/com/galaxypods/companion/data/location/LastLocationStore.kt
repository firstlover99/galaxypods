// 마지막 위치 1회 fetch + DataStore 영속화 — 백그라운드 추적 X
package com.galaxypods.companion.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.galaxypods.companion.data.preferences.AppPreferences
import com.galaxypods.companion.domain.model.LastLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * BLE 끊김 시점에 한 번만 GPS를 fetch해 [AppPreferences]에 저장한다.
 *
 * **CLAUDE.md 원칙 11 / 검토안 §2.2 — 백그라운드 위치 권한 회피 전략.**
 * `ACCESS_BACKGROUND_LOCATION`을 요구하지 않기 위해 FGS 컨텍스트 안에서
 * `getCurrentLocation()`만 호출. `requestLocationUpdates`는 사용하지 않음.
 *
 * **권한 사전 검사.** 미부여 시 silent return — 사용자가 끄는 것이 가능하도록.
 */
@Singleton
class LastLocationStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AppPreferences,
) {

    /**
     * 현재 위치를 1회 fetch해 영속화.
     *
     * @return 성공 시 [LastLocation], 권한 미부여 또는 위치 미사용 시 null.
     */
    suspend fun captureNow(): LastLocation? {
        if (!hasLocationPermission()) return null

        val location = getCurrentLocationOrNull() ?: return null
        val captured = LastLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = System.currentTimeMillis(),
            accuracyMeters = location.accuracy.takeIf { it > 0f },
        )
        preferences.setLastLocation(captured)
        return captured
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocationOrNull(): android.location.Location? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        return runCatching {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        }.getOrNull()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @Suppress("unused")
    private suspend fun fallbackForOldDevices() = suspendCancellableCoroutine<Unit> { cont ->
        // 오래된 단말 폴백 위치는 별도 작업 — 현 구현은 Google Play Services 의존
        cont.resume(Unit)
    }
}
