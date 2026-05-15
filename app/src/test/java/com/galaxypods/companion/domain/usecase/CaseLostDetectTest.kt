// CaseLostDetect — Haversine 거리 + 알림 결정 로직 검증
package com.galaxypods.companion.domain.usecase

import com.galaxypods.companion.domain.model.LastLocation
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CaseLostDetectTest {
    private lateinit var useCase: CaseLostDetect

    @BeforeEach
    fun setUp() {
        useCase = CaseLostDetect().apply { enabled = true }
    }

    @Test
    @DisplayName("Haversine — 같은 좌표는 0m")
    fun haversine_samePoint_isZero() {
        val d = useCase.haversineMeters(37.5665, 126.9780, 37.5665, 126.9780)
        assertThat(d).isWithin(0.001).of(0.0)
    }

    @Test
    @DisplayName("Haversine — 서울시청 ↔ 광화문 약 800m")
    fun haversine_seoulCityHallToGwanghwamun_approx800m() {
        val cityHall = Pair(37.5665, 126.9780)
        val gwanghwamun = Pair(37.5759, 126.9769)
        val d =
            useCase.haversineMeters(
                cityHall.first,
                cityHall.second,
                gwanghwamun.first,
                gwanghwamun.second,
            )
        assertThat(d).isWithin(50.0).of(1050.0) // 약 1km
    }

    @Test
    @DisplayName("disabled면 절대 알림 X")
    fun disabled_neverAlerts() {
        useCase.enabled = false
        val a = location(37.5665, 126.9780)
        val b = location(37.5759, 126.9769) // 약 1km 멀어짐
        val result = useCase.shouldAlert(a, b, stillDisconnected = true)
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("재연결 상태(stillDisconnected=false)면 알림 X")
    fun reconnected_doesNotAlert() {
        val a = location(37.5665, 126.9780)
        val b = location(37.5759, 126.9769)
        val result = useCase.shouldAlert(a, b, stillDisconnected = false)
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("거리 50m 미만이면 알림 X (사용자가 같은 자리)")
    fun shortDistance_doesNotAlert() {
        val a = location(37.5665, 126.9780)
        val b = location(37.5666, 126.9781) // 약 14m
        val result = useCase.shouldAlert(a, b, stillDisconnected = true)
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("거리 50m 이상 + 끊김 유지 → 알림")
    fun longDistance_andStillDisconnected_alerts() {
        val a = location(37.5665, 126.9780)
        val b = location(37.5670, 126.9785) // 약 70m
        val result = useCase.shouldAlert(a, b, stillDisconnected = true)
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("위치 데이터가 없으면 알림 X")
    fun nullLocation_doesNotAlert() {
        assertThat(useCase.shouldAlert(null, null, stillDisconnected = true)).isFalse()
        assertThat(useCase.shouldAlert(location(0.0, 0.0), null, stillDisconnected = true)).isFalse()
        assertThat(useCase.shouldAlert(null, location(0.0, 0.0), stillDisconnected = true)).isFalse()
    }

    private fun location(
        lat: Double,
        lng: Double,
        ts: Long = 0L,
    ): LastLocation = LastLocation(lat, lng, ts)
}
