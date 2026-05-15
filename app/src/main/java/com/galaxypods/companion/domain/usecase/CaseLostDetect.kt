// 케이스 분실 감지 UseCase — BLE 끊김 + 위치 변화 휴리스틱
package com.galaxypods.companion.domain.usecase

import com.galaxypods.companion.domain.model.LastLocation
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * "AirPods를 두고 가셨나요?" 분실 감지 휴리스틱.
 *
 * **차용 출처.** competitive-analysis §5 S급 4번. PodsLink/AndroPods는
 * "마지막 위치"만 표시. 우리는 끊김 후 사용자가 멀어지면 적극적으로 알림.
 *
 * **알고리즘.**
 * 1. BLE 연결 끊김 감지 시점 → [LastLocationStore.captureNow]가 위치 A 저장
 * 2. 일정 시간 (DELAY_MS) 뒤에도 BLE 미복구 → 위치 B 다시 fetch
 * 3. A와 B 거리 > [DISTANCE_THRESHOLD_M] → 분실 알림
 *
 * **사용처.** [com.galaxypods.companion.service.PodsForegroundService]가 끊김 트리거
 * 시 [shouldAlert] 호출. 위치 fetch는 호출자가 책임.
 *
 * **비활성 가능.** 사용자 옵트인 (`AppPreferences.caseLostAlertEnabled`).
 */
@Singleton
class CaseLostDetect
    @Inject
    constructor() {
        /** 사용자 설정. 향후 [com.galaxypods.companion.data.preferences.AppPreferences] 위임. */
        var enabled: Boolean = false

        /**
         * 분실 알림을 표시해야 하는지.
         *
         * @param disconnectedAt 연결 끊김 시점의 위치 (LastLocation)
         * @param currentLocation 현재 위치 (방금 fetch)
         * @param stillDisconnected BLE 여전히 미복구
         * @return true면 알림 표시
         */
        fun shouldAlert(
            disconnectedAt: LastLocation?,
            currentLocation: LastLocation?,
            stillDisconnected: Boolean,
        ): Boolean {
            if (!enabled) return false
            if (!stillDisconnected) return false
            if (disconnectedAt == null || currentLocation == null) return false

            val distance =
                haversineMeters(
                    lat1 = disconnectedAt.latitude,
                    lng1 = disconnectedAt.longitude,
                    lat2 = currentLocation.latitude,
                    lng2 = currentLocation.longitude,
                )
            return distance >= DISTANCE_THRESHOLD_M
        }

        /**
         * 두 좌표 사이 거리 (미터). Haversine 공식.
         * Android `Location.distanceTo()`도 사용 가능하나, 본 함수는 단위 테스트가
         * 안드로이드 의존성 없이 가능하도록 직접 구현.
         */
        @Suppress("MagicNumber")
        fun haversineMeters(
            lat1: Double,
            lng1: Double,
            lat2: Double,
            lng2: Double,
        ): Double {
            val earthRadiusM = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a =
                sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLng / 2) * sin(dLng / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadiusM * c
        }

        companion object {
            /** 끊김 후 분실 판정까지 대기 시간 (ms). */
            const val DELAY_MS: Long = 5 * 60 * 1000L

            /** 분실로 판정할 거리 임계값 (m). */
            const val DISTANCE_THRESHOLD_M: Double = 50.0
        }
    }
