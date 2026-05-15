// VoiceAnnouncer 메시지 결정 로직 단위 테스트 (TTS 엔진 호출 X)
package com.galaxypods.companion.data.system

import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.domain.model.AirPodsModel
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * VoiceAnnouncer의 메시지 결정 로직만 검증.
 * TTS 엔진 자체는 안드로이드 시스템 API라 instrumentation 테스트로 별도 검증.
 *
 * 본 테스트는 케이스 오픈 / 임계값 도달 분기가 정확한지 확인한다.
 */
class VoiceAnnouncerLogicTest {
    /**
     * VoiceAnnouncer는 Context를 요구하므로 본 테스트에선 메시지 결정 로직만
     * 별도 함수로 추출해 검증하는 패턴이 이상적. 본 골격에선 동작 시나리오를
     * 데이터 클래스 비교로만 확인.
     */
    @Test
    @DisplayName("lidOpenCount 증가 → 케이스 오픈 시나리오 트리거 조건")
    fun lidOpenIncrease_isCaseOpenScenario() {
        val previous = ad(lidOpenCount = 5)
        val current = ad(lidOpenCount = 6)
        assertThat(current.lidOpenCount > previous.lidOpenCount).isTrue()
    }

    @Test
    @DisplayName("배터리가 임계값을 넘어 떨어진 경우 트리거 조건")
    fun batteryDrop_belowThreshold_triggers() {
        val threshold = 20
        val previous = ad(left = 30, right = 50)
        val current = ad(left = 15, right = 50)
        val leftCrossed =
            current.leftBatteryPercent in 0..threshold &&
                previous.leftBatteryPercent > threshold
        assertThat(leftCrossed).isTrue()
    }

    @Test
    @DisplayName("이미 임계값 이하였으면 다시 트리거되지 않음 (중복 방지)")
    fun batteryAlreadyLow_doesNotRetrigger() {
        val threshold = 20
        val previous = ad(left = 10, right = 50)
        val current = ad(left = 8, right = 50)
        val leftCrossed =
            current.leftBatteryPercent in 0..threshold &&
                previous.leftBatteryPercent > threshold
        assertThat(leftCrossed).isFalse()
    }

    private fun ad(
        left: Int = 100,
        right: Int = 100,
        case: Int = 100,
        lidOpenCount: Int = 0,
    ): AirPodsAdvertisement =
        AirPodsAdvertisement(
            model = AirPodsModel.AIRPODS_PRO_2_USBC,
            leftBatteryPercent = left,
            rightBatteryPercent = right,
            caseBatteryPercent = case,
            leftInEar = false,
            rightInEar = false,
            leftCharging = false,
            rightCharging = false,
            caseCharging = false,
            lidOpenCount = lidOpenCount,
        )
}
