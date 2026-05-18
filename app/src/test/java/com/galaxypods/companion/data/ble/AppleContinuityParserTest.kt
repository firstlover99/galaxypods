// AppleContinuityParser 단위 테스트 — 합성 + 실측 dump 골든바이트 검증
package com.galaxypods.companion.data.ble

import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.domain.model.AirPodsModel
import com.galaxypods.companion.domain.model.LidState
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

// AppleContinuityParser 검증.
//
// 두 종류 테스트로 구성.
// 1. 합성 케이스 — 비트 오프셋 로직 자체의 정확성 검증. 필드별 단일 변경.
// 2. 실측 dump — CAPod 검증 벡터 + 자체 캡처 패킷으로 정확한 byte 위치/마스크 검증.
class AppleContinuityParserTest {
    private val parser =
        AppleContinuityParser(
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
    // 합성 페이로드 빌더 (v1.1 layout — CAPod 정렬)
    // ============================================================

    /**
     * Continuity 페이로드 (Type 0x07 + Length 0x19 + 25바이트) 합성.
     *
     * **레이아웃.**
     * [0] prefix=0x01, [1-2] device LE, [3] status, [4] pods battery,
     * [5] charging+case, [6] lid, [7] color, [8] suffix, [9+] encrypted
     */
    @Suppress("LongParameterList")
    private fun buildPayload(
        deviceType: Int = 0x2024,
        leftBatteryNibble: Int = 0x0A,
        rightBatteryNibble: Int = 0x09,
        caseBatteryNibble: Int = 0x05,
        leftInEar: Boolean = false,
        rightInEar: Boolean = false,
        leftCharging: Boolean = false,
        rightCharging: Boolean = false,
        caseCharging: Boolean = false,
        lidOpenCount: Int = 0,
        // status bit 5 (left primary) 기본 true → flip 안 함.
        leftPrimary: Boolean = true,
        thisPodInCase: Boolean = false,
    ): ByteArray {
        val payload = ByteArray(2 + ParserConfig.LENGTH_PROXIMITY_PAIRING)
        payload[0] = ParserConfig.TYPE_PROXIMITY_PAIRING.toByte()
        payload[1] = ParserConfig.LENGTH_PROXIMITY_PAIRING.toByte()

        val valueStart = 2
        // [0] prefix (실측 wire는 0x01 표준)
        payload[valueStart + 0] = 0x01
        // [1-2] Device Type LE
        payload[valueStart + 1] = (deviceType and 0xFF).toByte()
        payload[valueStart + 2] = ((deviceType shr 8) and 0xFF).toByte()

        // [3] Status byte — primary pod + in-ear + case position
        var statusByte = 0
        if (leftPrimary) statusByte = statusByte or ParserConfig.MASK_STATUS_LEFT_PRIMARY
        if (thisPodInCase) statusByte = statusByte or ParserConfig.MASK_STATUS_THIS_POD_IN_CASE
        // In-ear 매핑은 flip XOR isThisPodInCase 조건 따라 결정 (파서 로직 동일).
        val flipInEar = (!leftPrimary) xor thisPodInCase
        if (leftInEar) {
            statusByte =
                statusByte or
                if (flipInEar) {
                    ParserConfig.MASK_IN_EAR_DEFAULT_RIGHT
                } else {
                    ParserConfig.MASK_IN_EAR_DEFAULT_LEFT
                }
        }
        if (rightInEar) {
            statusByte =
                statusByte or
                if (flipInEar) {
                    ParserConfig.MASK_IN_EAR_DEFAULT_LEFT
                } else {
                    ParserConfig.MASK_IN_EAR_DEFAULT_RIGHT
                }
        }
        payload[valueStart + 3] = statusByte.toByte()

        // [4] Pods battery — flip 따라 nibble 위치 결정
        val highNibble =
            if (leftPrimary) {
                rightBatteryNibble and 0x0F
            } else {
                leftBatteryNibble and 0x0F
            }
        val lowNibble =
            if (leftPrimary) {
                leftBatteryNibble and 0x0F
            } else {
                rightBatteryNibble and 0x0F
            }
        payload[valueStart + 4] = ((highNibble shl 4) or lowNibble).toByte()

        // [5] Charging (upper nibble) + Case battery (lower nibble)
        var chargingCaseByte = caseBatteryNibble and 0x0F
        if (leftCharging) {
            chargingCaseByte =
                chargingCaseByte or
                if (leftPrimary) {
                    ParserConfig.MASK_DEFAULT_LEFT_CHARGING
                } else {
                    ParserConfig.MASK_DEFAULT_RIGHT_CHARGING
                }
        }
        if (rightCharging) {
            chargingCaseByte =
                chargingCaseByte or
                if (leftPrimary) {
                    ParserConfig.MASK_DEFAULT_RIGHT_CHARGING
                } else {
                    ParserConfig.MASK_DEFAULT_LEFT_CHARGING
                }
        }
        if (caseCharging) chargingCaseByte = chargingCaseByte or ParserConfig.MASK_CASE_CHARGING
        payload[valueStart + 5] = chargingCaseByte.toByte()

        // [6] Lid open count
        payload[valueStart + 6] = lidOpenCount.toByte()

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
        val payload =
            buildPayload(
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
        val payload =
            buildPayload(
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
    @DisplayName("AirPodsModelTable 통합 — AirPods Pro 2 USB-C 식별")
    fun parse_withRealModelTable_identifiesAirPodsPro2Usbc() {
        val realParser =
            AppleContinuityParser(
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

    @Test
    @DisplayName("충전 플래그 — 각 충전 옵션이 정확히 파싱됨 (case-charging 단독)")
    fun parse_caseChargingOnly() {
        val payload = buildPayload(caseCharging = true)
        val ad = parser.parse(payload)!!
        assertThat(ad.leftCharging).isFalse()
        assertThat(ad.rightCharging).isFalse()
        assertThat(ad.caseCharging).isTrue()
    }

    @Test
    @DisplayName("충전 플래그 — 양쪽 pod + case 모두 충전")
    fun parse_allCharging() {
        val payload =
            buildPayload(
                leftCharging = true,
                rightCharging = true,
                caseCharging = true,
            )
        val ad = parser.parse(payload)!!
        assertThat(ad.leftCharging).isTrue()
        assertThat(ad.rightCharging).isTrue()
        assertThat(ad.caseCharging).isTrue()
    }

    // ============================================================
    // 실측 dump 검증 (CAPod 벡터 + 자체 캡처)
    // ============================================================

    @Test
    @DisplayName("실측 dump 디렉토리가 존재한다 (수집 가이드 README 위치)")
    fun realDumpsDirectory_exists() {
        val resource = javaClass.classLoader?.getResource("ble_dumps/README.md")
        assertThat(resource).isNotNull()
    }

    @Test
    @DisplayName("CAPod 실측 #164 — AirPods Pro 2 USB-C, 양쪽 in-ear, 90% 양쪽, case unknown, 비충전")
    fun parse_capodVector_164_inEar() {
        val realParser =
            AppleContinuityParser(
                config = ParserConfig.DEFAULT,
                modelTable = AirPodsModelTable::identify,
            )
        // CAPod AirPodsPro2UsbcTest 첫번째 벡터 (27 bytes = TLV 2 + payload 25).
        val hex = "07190124200B998F11000400000000000000000000000000000000"
        val data = hexToBytes(hex)

        val ad = realParser.parse(data)
        assertThat(ad).isNotNull()
        ad!!
        assertThat(ad.model).isEqualTo(AirPodsModel.AIRPODS_PRO_2_USBC)
        assertThat(ad.leftBatteryPercent).isEqualTo(90)
        assertThat(ad.rightBatteryPercent).isEqualTo(90)
        // case nibble = F → unknown → -1
        assertThat(ad.caseBatteryPercent).isEqualTo(AirPodsAdvertisement.BATTERY_UNKNOWN)
        assertThat(ad.leftCharging).isFalse()
        assertThat(ad.rightCharging).isFalse()
        assertThat(ad.caseCharging).isFalse()
        // status 0x0B — pods 모두 귀에, case 컨텍스트 없음 → NOT_IN_CASE
        assertThat(ad.lidState).isEqualTo(LidState.NOT_IN_CASE)
        assertThat(ad.isThisPodInCase).isFalse()
        assertThat(ad.isOnePodInCase).isFalse()
        assertThat(ad.areBothPodsInCase).isFalse()
        // bit 5 = 0 → R primary
        assertThat(ad.isLeftPodPrimary).isFalse()
    }

    @Test
    @DisplayName("CAPod 실측 #164 in-case — AirPods Pro 2 USB-C, 100% 양쪽, case 80%, R 단독 충전")
    fun parse_capodVector_164_inCase() {
        val realParser =
            AppleContinuityParser(
                config = ParserConfig.DEFAULT,
                modelTable = AirPodsModelTable::identify,
            )
        // CAPod AirPodsPro2UsbcTest 두번째 벡터 (27 bytes).
        val hex = "071901242053AA983200050000000000000000000000000000000000"
        val data = hexToBytes(hex)

        val ad = realParser.parse(data)
        assertThat(ad).isNotNull()
        ad!!
        assertThat(ad.model).isEqualTo(AirPodsModel.AIRPODS_PRO_2_USBC)
        assertThat(ad.leftBatteryPercent).isEqualTo(100)
        assertThat(ad.rightBatteryPercent).isEqualTo(100)
        assertThat(ad.caseBatteryPercent).isEqualTo(80)
        assertThat(ad.leftCharging).isFalse()
        assertThat(ad.rightCharging).isTrue()
        assertThat(ad.caseCharging).isFalse()
        // status 0x53. bit 6=1 (this pod in case), bit 4=1 (one pod in case), bit 5=0 (R primary).
        assertThat(ad.isThisPodInCase).isTrue()
        assertThat(ad.isOnePodInCase).isTrue()
        assertThat(ad.areBothPodsInCase).isFalse()
        assertThat(ad.isLeftPodPrimary).isFalse()
        // lid byte 0x32 — bit 3 = 0 → OPEN (hasCaseContext = true 이므로).
        assertThat(ad.lidState).isEqualTo(LidState.OPEN)
    }

    @Test
    @DisplayName("실측 자체 캡처 — Note 20 재페어링 시 AirPods Pro 2 USB-C")
    fun parse_realCapture_airPodsPro2UsbcDuringRepair() {
        // 2026-05-18 Note 20 캡처. flip 적용 시 R=40%, L=unknown.
        val realParser =
            AppleContinuityParser(
                config = ParserConfig.DEFAULT,
                modelTable = AirPodsModelTable::identify,
            )
        val hex = "071907242015E4E444728D5EFABC29BDFB7FBC1481DEC3C6E327A9"
        val data = hexToBytes(hex)

        val ad = realParser.parse(data)
        assertThat(ad).isNotNull()
        ad!!
        assertThat(ad.model).isEqualTo(AirPodsModel.AIRPODS_PRO_2_USBC)
        // status 0x15. bit 5 = 0 → flipped (R primary). L/R 반전.
        // battery byte 0xE4. flip 적용: L=upper(E=14)=100% (CAPod 캡 동작),
        // R=lower(4)=40%.
        // (이전 파서는 11..14를 모두 UNKNOWN으로 처리했으나, CAPod 정렬로 변경.
        //  Apple 펌웨어가 "above 100%"로 0xB~0xE를 송출하는 경우 대응.)
        assertThat(ad.leftBatteryPercent).isEqualTo(100)
        assertThat(ad.rightBatteryPercent).isEqualTo(40)
        // case nibble = lower of 0xE4 = 4 → 40%.
        assertThat(ad.caseBatteryPercent).isEqualTo(40)
    }

    private fun hexToBytes(hex: String): ByteArray =
        hex.replace(" ", "")
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
}
