// Apple Continuity Proximity Pairing 광고 파서 — AirPods 상태 추출 코어 모듈
package com.galaxypods.companion.data.ble

import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.domain.model.AirPodsModel

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
 * **사용 예.**
 * ```
 * val payload = scanRecord.getManufacturerSpecificData(0x004C) ?: return
 * val ad = AppleContinuityParser.parse(payload, rssi = result.rssi)
 * ```
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
        val payloadStart = findProximityPairingPayload(data) ?: return null
        val end = payloadStart + config.expectedLength
        if (end > data.size) return null

        return parseProximityPairing(data, payloadStart, rssi, timestamp)
    }

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

        val battery1 = data[start + config.battery1Offset].toInt() and 0xFF
        val battery2 = data[start + config.battery2Offset].toInt() and 0xFF
        val chargingFlags = data[start + config.chargingOffset].toInt() and 0xFF
        val inEarFlags = data[start + config.inEarOffset].toInt() and 0xFF
        val lidOpenCount = data[start + config.lidOpenOffset].toInt() and 0xFF

        val rightNibble = (battery1 shr 4) and 0x0F
        val leftNibble = battery1 and 0x0F
        val caseNibble = battery2 and 0x0F

        return AirPodsAdvertisement(
            model = modelTable(deviceType),
            leftBatteryPercent = nibbleToPercent(leftNibble),
            rightBatteryPercent = nibbleToPercent(rightNibble),
            caseBatteryPercent = nibbleToPercent(caseNibble),
            leftInEar = (inEarFlags and ParserConfig.MASK_LEFT_IN_EAR) != 0,
            rightInEar = (inEarFlags and ParserConfig.MASK_RIGHT_IN_EAR) != 0,
            leftCharging = (chargingFlags and ParserConfig.MASK_LEFT_CHARGING) != 0,
            rightCharging = (chargingFlags and ParserConfig.MASK_RIGHT_CHARGING) != 0,
            caseCharging = (chargingFlags and ParserConfig.MASK_CASE_CHARGING) != 0,
            lidOpenCount = lidOpenCount,
            rssi = rssi,
            timestamp = timestamp,
        )
    }

    private fun readDeviceType(data: ByteArray, start: Int): Int {
        val high = data[start + config.deviceTypeOffset].toInt() and 0xFF
        val low = data[start + config.deviceTypeOffset + 1].toInt() and 0xFF
        return (high shl 8) or low
    }

    /**
     * 4비트 배터리 값(0~15)을 퍼센트로 변환.
     * 0xF는 "정보 없음" → BATTERY_UNKNOWN(-1).
     * 그 외 0~10은 그대로 *10 → 0~100. (광고는 10% 단위 정보만 제공)
     */
    private fun nibbleToPercent(nibble: Int): Int = when {
        nibble == ParserConfig.BATTERY_NIBBLE_UNKNOWN -> AirPodsAdvertisement.BATTERY_UNKNOWN
        nibble in 0..10 -> nibble * PERCENT_PER_STEP
        else -> AirPodsAdvertisement.BATTERY_UNKNOWN
    }

    companion object {
        private const val PERCENT_PER_STEP: Int = 10
    }
}
