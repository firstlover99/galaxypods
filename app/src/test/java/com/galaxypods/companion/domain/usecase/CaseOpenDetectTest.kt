// CaseOpenDetect 분기/멱등성/쿨다운 검증
package com.galaxypods.companion.domain.usecase

import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.domain.model.AirPodsModel
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CaseOpenDetectTest {
    private lateinit var useCase: CaseOpenDetect

    @BeforeEach
    fun setUp() {
        useCase = CaseOpenDetect()
    }

    @Test
    @DisplayName("첫 광고는 기준선만, 트리거 X")
    fun firstAd_baseline_noTrigger() {
        var triggered = false
        useCase.onAdvertisement(adWith(lidOpen = 5)) { triggered = true }
        assertThat(triggered).isFalse()
    }

    @Test
    @DisplayName("lidOpenCount 증가 → 트리거")
    fun lidOpenIncrease_triggers() {
        var triggered = false
        var triggeredAd: AirPodsAdvertisement? = null
        useCase.onAdvertisement(adWith(lidOpen = 5)) {}
        useCase.onAdvertisement(adWith(lidOpen = 6)) {
            triggered = true
            triggeredAd = it
        }
        assertThat(triggered).isTrue()
        assertThat(triggeredAd?.lidOpenCount).isEqualTo(6)
    }

    @Test
    @DisplayName("같은 카운트는 트리거 X")
    fun sameCount_noTrigger() {
        useCase.onAdvertisement(adWith(lidOpen = 5)) {}
        var count = 0
        useCase.onAdvertisement(adWith(lidOpen = 5)) { count++ }
        useCase.onAdvertisement(adWith(lidOpen = 5)) { count++ }
        assertThat(count).isEqualTo(0)
    }

    @Test
    @DisplayName("카운트 감소 (롤오버 또는 새 케이스)는 트리거 X, 기준선 재설정")
    fun countDecrease_resetsBaseline() {
        useCase.onAdvertisement(adWith(lidOpen = 200)) {}
        var triggered = false
        useCase.onAdvertisement(adWith(lidOpen = 1)) { triggered = true }
        assertThat(triggered).isFalse()

        // 다음 증가는 정상 트리거
        useCase.onAdvertisement(adWith(lidOpen = 2)) { triggered = true }
        assertThat(triggered).isTrue()
    }

    @Test
    @DisplayName("쿨다운 안에서 두 번째 증가는 트리거 X")
    fun cooldown_blocksRapidTriggers() {
        var count = 0
        useCase.onAdvertisement(adWith(lidOpen = 5), nowMs = 1000L) {}
        useCase.onAdvertisement(adWith(lidOpen = 6), nowMs = 1500L) { count++ } // 첫 트리거
        useCase.onAdvertisement(adWith(lidOpen = 7), nowMs = 1700L) { count++ } // 쿨다운 안 (200ms) → 차단
        useCase.onAdvertisement(adWith(lidOpen = 8), nowMs = 4000L) { count++ } // 쿨다운 지남 → 트리거
        assertThat(count).isEqualTo(2)
    }

    @Test
    @DisplayName("enabled=false면 트리거 X (그러나 기준선 추적)")
    fun disabled_noTrigger_butTracks() {
        useCase.enabled = false
        var count = 0
        useCase.onAdvertisement(adWith(lidOpen = 5)) {}
        useCase.onAdvertisement(adWith(lidOpen = 6)) { count++ }
        assertThat(count).isEqualTo(0)

        // 다시 활성화 후 즉시 다음 광고는 기준선이라 트리거 X
        useCase.enabled = true
        useCase.onAdvertisement(adWith(lidOpen = 7)) { count++ }
        useCase.onAdvertisement(adWith(lidOpen = 8)) { count++ }
        assertThat(count).isEqualTo(1)
    }

    @Test
    @DisplayName("reset 후 다음 광고는 기준선")
    fun reset_clearsState() {
        useCase.onAdvertisement(adWith(lidOpen = 5)) {}
        useCase.reset()
        var triggered = false
        useCase.onAdvertisement(adWith(lidOpen = 6)) { triggered = true }
        assertThat(triggered).isFalse()
    }

    private fun adWith(lidOpen: Int): AirPodsAdvertisement =
        AirPodsAdvertisement(
            model = AirPodsModel.AIRPODS_PRO_2_USBC,
            leftBatteryPercent = 80,
            rightBatteryPercent = 80,
            caseBatteryPercent = 80,
            leftInEar = false,
            rightInEar = false,
            leftCharging = false,
            rightCharging = false,
            caseCharging = false,
            lidOpenCount = lidOpen,
        )
}
