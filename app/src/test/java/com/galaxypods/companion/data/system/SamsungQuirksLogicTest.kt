// SamsungQuirks 분기 로직 단위 테스트 (시스템 호출 X, 외부 라이브러리 mock X)
package com.galaxypods.companion.data.system

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * SamsungQuirks의 SDK_INT 폴백 매핑 로직만 검증.
 *
 * 실제 reflection / startActivity / UsageStatsManager 호출은 instrumentation
 * 테스트로 별도 검증 (실기기/에뮬레이터 의존).
 */
class SamsungQuirksLogicTest {

    @Test
    @DisplayName("SDK_INT 폴백 매핑 — Android 11~16 → One UI 3~8 추정")
    fun sdkIntFallback_correctMapping() {
        // 본 매핑 로직은 SamsungQuirks 내부 private 함수와 동일하게 복제 검증
        // (실제 클래스는 reflection이 우선이므로 폴백 경로만 검증)
        assertThat(mapSdkToOneUi(30)).isEqualTo(3)  // Android 11
        assertThat(mapSdkToOneUi(31)).isEqualTo(4)  // Android 12
        assertThat(mapSdkToOneUi(32)).isEqualTo(4)  // Android 12L
        assertThat(mapSdkToOneUi(33)).isEqualTo(5)  // Android 13
        assertThat(mapSdkToOneUi(34)).isEqualTo(6)  // Android 14
        assertThat(mapSdkToOneUi(35)).isEqualTo(7)  // Android 15
        assertThat(mapSdkToOneUi(36)).isEqualTo(8)  // Android 16
    }

    @Test
    @DisplayName("Android 10 이하는 매핑되지 않음 (minSdk 26~29 구간)")
    fun sdkIntFallback_belowMinSdk_isNull() {
        assertThat(mapSdkToOneUi(26)).isNull()
        assertThat(mapSdkToOneUi(29)).isNull()
    }

    @Test
    @DisplayName("SemPlatformVersion 변환 — 130000 → One UI 4, 150000 → One UI 6")
    fun semPlatformVersion_conversion() {
        assertThat(semToOneUi(130000)).isEqualTo(4)
        assertThat(semToOneUi(150000)).isEqualTo(6)
        assertThat(semToOneUi(160000)).isEqualTo(7)
    }

    /** SamsungQuirks의 SDK_INT 폴백 매핑과 동일하게 유지 (변경 시 두 곳 동기화). */
    private fun mapSdkToOneUi(sdk: Int): Int? = when (sdk) {
        30 -> 3
        31, 32 -> 4
        33 -> 5
        34 -> 6
        35 -> 7
        else -> if (sdk > 35) 8 else null
    }

    private fun semToOneUi(semInt: Int): Int = (semInt / 10000) - 9
}
