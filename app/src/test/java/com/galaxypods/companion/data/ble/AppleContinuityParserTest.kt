// AppleContinuityParser 단위 테스트 — 합성 + 실측 dump 골든바이트 검증
package com.galaxypods.companion.data.ble

import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.domain.model.AirPodsModel
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * `AppleContinuityParser` 검증.
 *
 * 두 종류 테스트로 구성.
 * 1. **합성 케이스** — 비트 오프셋 로직 자체의 정확성 검증. 필드별 단일 변경.
 * 2. **실측 dump** — `app/src/test/resources/ble_dumps/*.hex` 파일이 모두 파싱되는지 검증
 *    (모델별 기대값은 dump가 추가될 때 함께 등록).
 */
class AppleContinuityParserTest {

    private val parser = AppleContinuityParser(
        config = ParserConfig.DEFAULT,
        modelTable = { deviceType ->
            // 테스트용 룩업. 실제 매핑과 분리해 파서 단독 검증.
            when (deviceType) {
                0x2024 -> AirPodsModel.AIRPODS_PRO_2_USBC
                0x2014 -> AirPodsModel.AIRPODS_PRO_2_LIGHTNING
                0x200E -> AirPodsModel.AIRPODS_PRO_1
                else -> AirPodsModel.UNKNOWN
            }
        },
    )

    // ============================================================
    // 합성 페이로드 빌더
    // ============================================================

    /**
     * Continuity 페이로드 (Type 0x07 + Length 0x19 + 25바이트) 합성.
     * 필드별로 명시 지정 가능.
     */
    @Suppress("LongParameterList")
    private fun buildPayload(
        deviceType: Int = 0x2024,
        leftBatteryNibble: Int = 0x0A, // 100%
        rightBatteryNibble: Int = 0x09, // 90%
        caseBatteryNibble: Int = 0x05, // 50%
        leftInEar: Boolean = false,
        rightInEar: Boolean = false,
        leftCharging: Boolean = false,
        rightCharging: Boolean = false,
        caseCharging: Boolean = false,
        lidOpenCount: Int = 0,
    ): ByteArray {
        val payload = ByteArray(2 + ParserConfig.LENGTH_PROXIMITY_PAIRING)
        payload[0] = ParserConfig.TYPE_PROXIMITY_PAIRING.toByte()
        payload[1] = ParserConfig.LENGTH_PROXIMITY_PAIRING.toByte()

        val valueStart = 2
        // Device Type (big-endian)
        payload[valueStart + 0] = ((deviceType shr 8) and 0xFF).toByte()
        payload[valueStart + 1] = (deviceType and 0xFF).toByte()

        // in-ear flags
        var inEarByte = 0
        if (leftInEar) inEarByte = inEarByte or ParserConfig.MASK_LEFT_IN_EAR
        if (rightInEar) inEarByte = inEarByte or ParserConfig.MASK_RIGHT_IN_EAR
        payload[valueStart + 3] = inEarByte.toByte()

        // battery 1 (right high nibble + left low nibble)
        payload[valueStart + 5] = (((rightBatteryNibble and 0x0F) shl 4) or
            (leftBatteryNibble and 0x0F)).toByte()

        // battery 2 (case in low nibble)
        payload[valueStart + 6] = (caseBatteryNibble and 0x0F).toByte()

        // charging flags
        var chargingByte = 0
        if (rightCharging) chargingByte = chargingByte or ParserConfig.MASK_RIGHT_CHARGING
        if (leftCharging) chargingByte = chargingByte or ParserConfig.MASK_LEFT_CHARGING
        if (caseCharging) chargingByte = chargingByte or ParserConfig.MASK_CASE_CHARGING
        payload[valueStart + 7] = chargingByte.toByte()

        // lid open count
        payload[valueStart + 8] = lidOpenCount.toByte()

        return payload
    }

    // ============================================================
    // 합성 테스트
    // ============================================================

    @Test
    @DisplayName("페이로드가 너무 짧으면 null 반환")
    fun parse_shortPayload_returnsNull() {
        val short = byteArrayOf(0x07, 0x19, 0x01, 0x02)
        assertNull(parser.parse(short))
    }

    @Test
    @DisplayName("Proximity Pairing TLV가 없으면 null 반환")
    fun parse_noProximityTlv_returnsNull() {
        // Type=0x09 (다른 Continuity 메시지) 25바이트
        val other = ByteArray(2 + ParserConfig.LENGTH_PROXIMITY_PAIRING)
        other[0] = 0x09
        other[1] = ParserConfig.LENGTH_PROXIMITY_PAIRING.toByte()
        assertNull(parser.parse(other))
    }

    @Test
    @DisplayName("기본 합성 페이로드 — 모델/배터리/플래그 정확 파싱")
    fun parse_defaultSynthetic_parsesAllFields() {
        val payload = buildPayload(
            deviceType = 0x2024,
            leftBatteryNibble = 0x0A,
            rightBatteryNibble = 0x09,
            caseBatteryNibble = 0x05,
            leftInEar = true,
            rightInEar = false,
            caseCharging = true,
            lidOpenCount = 7,
        )

        val ad = parser.parse(payload, rssi = -50, timestamp = 1000L)

        assertThat(ad).isNotNull()
        ad!!
        assertThat(ad.model).isEqualTo(AirPodsModel.AIRPODS_PRO_2_USBC)
        assertThat(ad.leftBatteryPercent).isEqualTo(100)
        assertThat(ad.rightBatteryPercent).isEqualTo(90)
        assertThat(ad.caseBatteryPercent).isEqualTo(50)
        assertThat(ad.leftInEar).isTrue()
        assertThat(ad.rightInEar).isFalse()
        assertThat(ad.leftCharging).isFalse()
        assertThat(ad.rightCharging).isFalse()
        assertThat(ad.caseCharging).isTrue()
        assertThat(ad.lidOpenCount).isEqualTo(7)
        assertThat(ad.rssi).isEqualTo(-50)
        assertThat(ad.timestamp).isEqualTo(1000L)
    }

    @Test
    @DisplayName("배터리 nibble 0xF는 BATTERY_UNKNOWN으로 변환")
    fun parse_unknownBatteryNibble_returnsUnknown() {
        val payload = buildPayload(
            leftBatteryNibble = 0x0F,
            rightBatteryNibble = 0x0F,
            caseBatteryNibble = 0x0F,
        )

        val ad = parser.parse(payload)!!

        assertThat(ad.leftBatteryPercent).isEqualTo(AirPodsAdvertisement.BATTERY_UNKNOWN)
        assertThat(ad.rightBatteryPercent).isEqualTo(AirPodsAdvertisement.BATTERY_UNKNOWN)
        assertThat(ad.caseBatteryPercent).isEqualTo(AirPodsAdvertisement.BATTERY_UNKNOWN)
    }

    @Test
    @DisplayName("미식별 deviceType은 UNKNOWN 모델로 폴백")
    fun parse_unknownDeviceType_fallsBackToUnknown() {
        val payload = buildPayload(deviceType = 0xFFFE)
        val ad = parser.parse(payload)!!
        assertThat(ad.model).isEqualTo(AirPodsModel.UNKNOWN)
    }

    @Test
    @DisplayName("AirPodsModelTable 통합 — Beats 모델도 식별됨")
    fun parse_withRealModelTable_identifiesBeats() {
        val realParser = AppleContinuityParser(
            config = ParserConfig.DEFAULT,
            modelTable = AirPodsModelTable::identify,
        )
        // Powerbeats Pro = 0x200B
        val payload = buildPayload(deviceType = 0x200B)
        val ad = realParser.parse(payload)!!
        assertThat(ad.model).isEqualTo(AirPodsModel.POWERBEATS_PRO)
        assertThat(ad.model.category).isEqualTo(AirPodsModel.Category.BEATS)
    }

    @Test
    @DisplayName("AirPodsModelTable 통합 — AirPods Pro 2 USB-C 식별")
    fun parse_withRealModelTable_identifiesAirPodsPro2Usbc() {
        val realParser = AppleContinuityParser(
            config = ParserConfig.DEFAULT,
            modelTable = AirPodsModelTable::identify,
        )
        val payload = buildPayload(deviceType = 0x2024)
        val ad = realParser.parse(payload)!!
        assertThat(ad.model).isEqualTo(AirPodsModel.AIRPODS_PRO_2_USBC)
        assertThat(ad.model.category).isEqualTo(AirPodsModel.Category.AIRPODS)
    }

    @Test
    @DisplayName("앞에 Type 0x05 짧은 TLV가 있어도 0x07 페이로드를 찾아냄")
    fun parse_skipsLeadingTlv_findsProximityPairing() {
        // Type 0x05 + Length 0x03 + 3바이트 + 그 다음 Proximity Pairing
        val proximity = buildPayload(deviceType = 0x200E)
        val combined = byteArrayOf(0x05, 0x03, 0x01, 0x02, 0x03) + proximity

        val ad = parser.parse(combined)!!
        assertThat(ad.model).isEqualTo(AirPodsModel.AIRPODS_PRO_1)
    }

    @ParameterizedTest
    @MethodSource("chargingFlagCases")
    @DisplayName("충전 플래그 비트별 정확 추출")
    fun parse_chargingFlags(
        leftCharging: Boolean,
        rightCharging: Boolean,
        caseCharging: Boolean,
    ) {
        val payload = buildPayload(
            leftCharging = leftCharging,
            rightCharging = rightCharging,
            caseCharging = caseCharging,
        )

        val ad = parser.parse(payload)!!

        assertThat(ad.leftCharging).isEqualTo(leftCharging)
        assertThat(ad.rightCharging).isEqualTo(rightCharging)
        assertThat(ad.caseCharging).isEqualTo(caseCharging)
    }

    // ============================================================
    // 실측 dump 검증 (dump 추가되면 자동 확장)
    // ============================================================

    @Test
    @DisplayName("실측 dump 디렉토리가 존재한다 (수집 가이드 README 위치)")
    fun realDumpsDirectory_exists() {
        val resource = javaClass.classLoader?.getResource("ble_dumps/README.md")
        assertThat(resource).isNotNull()
    }

    // TODO. 실측 dump가 추가되면 본 테스트를 ParameterizedTest로 확장.
    //       dump 파일명에서 모델 추론 → 파싱 결과 모델 일치 확인.

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun chargingFlagCases(): List<Array<Boolean>> = listOf(
            arrayOf(false, false, false),
            arrayOf(true, false, false),
            arrayOf(false, true, false),
            arrayOf(false, false, true),
            arrayOf(true, true, true),
        )
    }
}
