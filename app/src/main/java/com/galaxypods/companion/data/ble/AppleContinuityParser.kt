// Apple Continuity Proximity Pairing 광고 파서 — AirPods 상태 추출 코어 모듈
package com.galaxypods.companion.data.ble

import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.domain.model.AirPodsModel
import com.galaxypods.companion.domain.model.LidState

/**
 * Apple Continuity 광고에서 AirPods 상태를 파싱한다.
 *
 * 입력은 ScanRecord.getManufacturerSpecificData(0x004C)로 얻은 바이트 배열.
 * 본 파서는 외부 라이브러리에 의존하지 않고 Continuity TLV 구조만 직접 파싱.
 *
 * **격리 원칙 (CLAUDE.md 원칙 1).**
 * - 모든 비트 오프셋은 ParserConfig로 분리 → 펌웨어 변경 시 코드 수정 X.
 * - 본 파서 변경 PR은 반드시 골든바이트 테스트 갱신 동반.
 *
 * **참조.** CAPod (`d4rken-org/capod`) DualApplePods 파싱 로직 + furiousMAC continuity 사양.
 * 코드 복사 X — 로직만 참조해 독자 구현 (CLAUDE.md 원칙 3 준수).
 */
class AppleContinuityParser(
    private val config: ParserConfig = ParserConfig.DEFAULT,
    private val modelTable: (Int) -> AirPodsModel = AirPodsModelTable::identify,
) {
    /**
     * Continuity 페이로드를 파싱.
     *
     * @param data manufacturer-specific data (0x004C 매뉴팩처러 데이터, TLV 묶음).
     * @param rssi 광고 신호 강도 (옵션).
     * @param timestamp 광고 수신 시각 (옵션, ms).
     * @return 파싱 성공 시 [AirPodsAdvertisement], Proximity Pairing TLV가 없거나
     *         페이로드 길이가 부족하면 null.
     */
    fun parse(
        data: ByteArray,
        rssi: Int = 0,
        timestamp: Long = 0L,
    ): AirPodsAdvertisement? {
        // Type 0x07 (Proximity Pairing) — 배터리/모델/in-ear 모두
        val payloadStart = findProximityPairingPayload(data)
        if (payloadStart != null) {
            val end = payloadStart + config.expectedLength
            if (end <= data.size) {
                return parseProximityPairing(data, payloadStart, rssi, timestamp)
            }
        }

        // Type 0x10 (Nearby Info) fallback — 페어링 active 상태에서 받는 광고
        // 배터리/모델 정보 없음. 단순 "Apple 기기 근처에 있음"만 표시.
        if (hasNearbyInfo(data)) {
            return nearbyInfoFallback(rssi, timestamp)
        }

        return null
    }

    /** Type 0x10 (Nearby Info) TLV가 포함되어 있는지. */
    private fun hasNearbyInfo(data: ByteArray): Boolean {
        var offset = 0
        while (offset + 1 < data.size) {
            val type = data[offset].toInt() and 0xFF
            val length = data[offset + 1].toInt() and 0xFF
            if (type == TYPE_NEARBY_INFO) return true
            offset += 2 + length
        }
        return false
    }

    /**
     * Nearby Info 광고는 배터리/모델 정보를 포함하지 않음.
     * "Apple 기기 발견" 수준의 placeholder 표현. 사용자는 케이스 뚜껑 열기 또는
     * 페어링 모드 진입을 통해 Type 0x07를 받아야 배터리 표시.
     */
    private fun nearbyInfoFallback(
        rssi: Int,
        timestamp: Long,
    ): AirPodsAdvertisement =
        AirPodsAdvertisement(
            model = AirPodsModel.UNKNOWN,
            leftBatteryPercent = AirPodsAdvertisement.BATTERY_UNKNOWN,
            rightBatteryPercent = AirPodsAdvertisement.BATTERY_UNKNOWN,
            caseBatteryPercent = AirPodsAdvertisement.BATTERY_UNKNOWN,
            leftInEar = false,
            rightInEar = false,
            leftCharging = false,
            rightCharging = false,
            caseCharging = false,
            lidOpenCount = 0,
            rssi = rssi,
            timestamp = timestamp,
        )

    /**
     * TLV를 순회해 Type=0x07 (Proximity Pairing) 페이로드의 시작 인덱스를 찾는다.
     *
     * 각 TLV는 [Type(1), Length(1), Value(Length)] 구조. Length가 expectedLength보다
     * 짧으면 무시(부분 광고 또는 다른 Continuity 메시지).
     */
    private fun findProximityPairingPayload(data: ByteArray): Int? {
        var offset = 0
        while (offset + 1 < data.size) {
            val type = data[offset].toInt() and 0xFF
            val length = data[offset + 1].toInt() and 0xFF
            val payloadStart = offset + 2

            if (type == config.proximityPairingType && length >= config.expectedLength) {
                return payloadStart
            }
            offset = payloadStart + length
        }
        return null
    }

    private fun parseProximityPairing(
        data: ByteArray,
        start: Int,
        rssi: Int,
        timestamp: Long,
    ): AirPodsAdvertisement {
        val deviceType = readDeviceType(data, start)
        val statusByte = data[start + config.statusOffset].toInt() and 0xFF
        val podsBatteryByte = data[start + config.podsBatteryOffset].toInt() and 0xFF
        val chargingCaseByte = data[start + config.chargingCaseBatteryOffset].toInt() and 0xFF
        val lidOpenCount = data[start + config.lidOpenOffset].toInt() and 0xFF

        // Status bit 5 = primary pod (1 = L primary). 0이면 R primary → battery/charging 반전.
        // CAPod DualApplePods 참조.
        val isLeftPrimary = (statusByte and ParserConfig.MASK_STATUS_LEFT_PRIMARY) != 0
        val areValuesFlipped = !isLeftPrimary
        val isThisPodInCase = (statusByte and ParserConfig.MASK_STATUS_THIS_POD_IN_CASE) != 0
        val isOnePodInCase = (statusByte and ParserConfig.MASK_STATUS_ONE_POD_IN_CASE) != 0
        val areBothPodsInCase = (statusByte and ParserConfig.MASK_STATUS_BOTH_PODS_IN_CASE) != 0
        val hasCaseContext = isThisPodInCase || isOnePodInCase || areBothPodsInCase

        // LidState — CAPod fromRaw 정렬.
        val lidState = LidState.fromRaw(lidOpenCount, hasCaseContext)

        // Pods 배터리 — flip 적용
        val highNibble = (podsBatteryByte shr 4) and 0x0F
        val lowNibble = podsBatteryByte and 0x0F
        val leftNibble = if (areValuesFlipped) highNibble else lowNibble
        val rightNibble = if (areValuesFlipped) lowNibble else highNibble

        // 케이스 배터리 — lower nibble of chargingCaseByte (flip 무관)
        val caseNibble = chargingCaseByte and 0x0F

        // 충전 플래그 — flip 적용 (case는 flip 무관)
        val leftCharging =
            if (areValuesFlipped) {
                (chargingCaseByte and ParserConfig.MASK_DEFAULT_RIGHT_CHARGING) != 0
            } else {
                (chargingCaseByte and ParserConfig.MASK_DEFAULT_LEFT_CHARGING) != 0
            }
        val rightCharging =
            if (areValuesFlipped) {
                (chargingCaseByte and ParserConfig.MASK_DEFAULT_LEFT_CHARGING) != 0
            } else {
                (chargingCaseByte and ParserConfig.MASK_DEFAULT_RIGHT_CHARGING) != 0
            }
        val caseCharging = (chargingCaseByte and ParserConfig.MASK_CASE_CHARGING) != 0

        // In-ear — flip XOR isThisPodInCase로 매핑 결정 (CAPod DualApplePods)
        val flipInEar = areValuesFlipped xor isThisPodInCase
        val leftInEar =
            if (flipInEar) {
                (statusByte and ParserConfig.MASK_IN_EAR_DEFAULT_RIGHT) != 0
            } else {
                (statusByte and ParserConfig.MASK_IN_EAR_DEFAULT_LEFT) != 0
            }
        val rightInEar =
            if (flipInEar) {
                (statusByte and ParserConfig.MASK_IN_EAR_DEFAULT_LEFT) != 0
            } else {
                (statusByte and ParserConfig.MASK_IN_EAR_DEFAULT_RIGHT) != 0
            }

        return AirPodsAdvertisement(
            model = modelTable(deviceType),
            leftBatteryPercent = nibbleToPercent(leftNibble),
            rightBatteryPercent = nibbleToPercent(rightNibble),
            caseBatteryPercent = nibbleToPercent(caseNibble),
            leftInEar = leftInEar,
            rightInEar = rightInEar,
            leftCharging = leftCharging,
            rightCharging = rightCharging,
            caseCharging = caseCharging,
            lidOpenCount = lidOpenCount,
            lidState = lidState,
            isThisPodInCase = isThisPodInCase,
            isOnePodInCase = isOnePodInCase,
            areBothPodsInCase = areBothPodsInCase,
            isLeftPodPrimary = isLeftPrimary,
            rssi = rssi,
            timestamp = timestamp,
        )
    }

    /**
     * Device Type 2바이트를 **little-endian**으로 읽는다.
     *
     * Apple Continuity 와이어 레이아웃에서 device type은 [low, high] 순서로 송신.
     * 예. AirPods Pro 2 USB-C → wire `24 20` → value `0x2024`.
     */
    private fun readDeviceType(
        data: ByteArray,
        start: Int,
    ): Int {
        val low = data[start + config.deviceTypeOffset].toInt() and 0xFF
        val high = data[start + config.deviceTypeOffset + 1].toInt() and 0xFF
        return (high shl 8) or low
    }

    /**
     * 4비트 배터리 값(0~15)을 퍼센트로 변환.
     * 0xF는 "정보 없음" → BATTERY_UNKNOWN(-1).
     * 0~10은 그대로 *10 → 0~100. 10 초과는 100으로 캡 (펌웨어가 가끔 11+ 보냄, CAPod 동일 처리).
     */
    private fun nibbleToPercent(nibble: Int): Int =
        when {
            nibble == ParserConfig.BATTERY_NIBBLE_UNKNOWN -> AirPodsAdvertisement.BATTERY_UNKNOWN
            nibble in 0..10 -> nibble * PERCENT_PER_STEP
            nibble in 11..14 -> 100 // CAPod 동일 처리. 펌웨어 변동 대응.
            else -> AirPodsAdvertisement.BATTERY_UNKNOWN
        }

    companion object {
        private const val PERCENT_PER_STEP: Int = 10

        /** Continuity Nearby Info TLV. 페어링 active 상태에서 송출되며 배터리 정보 없음. */
        private const val TYPE_NEARBY_INFO: Int = 0x10
    }
}
