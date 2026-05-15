// BatteryIcon.formatPercent — 텍스트 포맷팅 검증 (Bitmap 렌더링은 instrumentation)
package com.galaxypods.companion.service

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class BatteryIconTest {
    @Test
    @DisplayName("0~100 정상 범위는 그대로 문자열")
    fun normalRange() {
        assertThat(BatteryIcon.formatPercent(0)).isEqualTo("0")
        assertThat(BatteryIcon.formatPercent(50)).isEqualTo("50")
        assertThat(BatteryIcon.formatPercent(100)).isEqualTo("100")
    }

    @Test
    @DisplayName("음수는 — (정보 없음)")
    fun negative_isUnknown() {
        assertThat(BatteryIcon.formatPercent(-1)).isEqualTo("—")
        assertThat(BatteryIcon.formatPercent(Int.MIN_VALUE)).isEqualTo("—")
    }

    @Test
    @DisplayName("100 초과는 100으로 클램프 (광고 노이즈 방어)")
    fun overflow_clampedToHundred() {
        assertThat(BatteryIcon.formatPercent(150)).isEqualTo("100")
        assertThat(BatteryIcon.formatPercent(Int.MAX_VALUE)).isEqualTo("100")
    }

    @ParameterizedTest
    @CsvSource(
        "0, 0",
        "5, 5",
        "10, 10",
        "85, 85",
        "100, 100",
    )
    @DisplayName("자릿수별 정확 포맷팅")
    fun digitVariants(
        input: Int,
        expected: String,
    ) {
        assertThat(BatteryIcon.formatPercent(input)).isEqualTo(expected)
    }
}
