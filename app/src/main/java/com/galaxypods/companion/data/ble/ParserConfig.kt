// AppleContinuityParser 비트 오프셋 설정 — 펌웨어 변경 시 코드 수정 없이 교체 가능
package com.galaxypods.companion.data.ble

/**
 * Apple Continuity Proximity Pairing 페이로드 내 필드 오프셋.
 *
 * Apple이 펌웨어 업데이트로 필드 위치를 옮길 경우, 코드 변경 없이 본 데이터 클래스만
 * 교체(추후 Firebase Remote Config로 `parser_config.json` 다운로드)하면 동작 복구 가능.
 *
 * **레이아웃 (CAPod / furiousMAC continuity 검증).**
 * ```
 * [0]   prefix (0x01 표준, 일부 펌웨어 0x07)
 * [1-2] device model (little-endian)
 * [3]   status — in-ear, case 위치, primary pod (flip bit)
 * [4]   pods battery — upper nibble=R, lower nibble=L (flip시 반전)
 * [5]   charging (upper nibble) + case battery (lower nibble)
 * [6]   lid open counter
 * [7]   device color
 * [8]   suffix (0x00)
 * [9+]  encrypted IRK (16바이트)
 * ```
 *
 * **검증.** `app/src/test/resources/ble_dumps/` 골든바이트 + CAPod 실측 테스트 벡터.
 */
data class ParserConfig(
    val version: String = "v1.1-capod-aligned",
    val proximityPairingType: Int = TYPE_PROXIMITY_PAIRING,
    val expectedLength: Int = LENGTH_PROXIMITY_PAIRING,
    /**
     * Device Type 2바이트 시작 오프셋.
     *
     * payload[1..2] = Device Type (**little-endian**) ← 본 오프셋
     * 읽기는 LE: payload[offset]=low, payload[offset+1]=high.
     */
    val deviceTypeOffset: Int = 1,
    /**
     * Status byte 오프셋.
     *
     * - bit 5: primary pod (1=L primary, 0=R primary). R primary이면 battery/charging 반전.
     * - bit 6: this pod (broadcaster) in case
     * - bit 4: one pod in case
     * - bit 2: both pods in case
     * - bit 3, bit 1: in-ear (조건부 매핑, CAPod DualApplePods 참조)
     */
    val statusOffset: Int = 3,
    /** Pods 배터리 1바이트 (R/L nibble, flip 적용). */
    val podsBatteryOffset: Int = 4,
    /** 충전 플래그 (upper nibble) + 케이스 배터리 (lower nibble) 통합 byte. */
    val chargingCaseBatteryOffset: Int = 5,
    /** 케이스 lid open 카운터 (변화량으로 케이스 오픈 감지). */
    val lidOpenOffset: Int = 6,
    /** Device color byte. */
    val colorOffset: Int = 7,
) {
    companion object {
        /** Apple Inc. — Bluetooth SIG 할당 manufacturer ID. */
        const val APPLE_MANUFACTURER_ID: Int = 0x004C

        /** Continuity TLV 타입. AirPods 근접 페어링 광고. */
        const val TYPE_PROXIMITY_PAIRING: Int = 0x07

        /** Type 0x07 페이로드 길이. 0x19 = 25 바이트. */
        const val LENGTH_PROXIMITY_PAIRING: Int = 0x19

        /** Status bit. primary pod (1 = left primary, 0 = right primary → flip). */
        const val MASK_STATUS_LEFT_PRIMARY: Int = 0x20

        /** Status bit. this broadcasting pod is in the case. */
        const val MASK_STATUS_THIS_POD_IN_CASE: Int = 0x40

        /**
         * In-ear 비트 (status byte). CAPod 조건 매핑.
         * `areValuesFlipped XOR isThisPodInThecase == false` 기준.
         * - L 정상: bit 1 (0x02)
         * - R 정상: bit 3 (0x08)
         * 조건이 true이면 L/R 교환.
         */
        const val MASK_IN_EAR_DEFAULT_LEFT: Int = 0x02
        const val MASK_IN_EAR_DEFAULT_RIGHT: Int = 0x08

        /**
         * 충전 플래그 (chargingCaseBattery byte의 upper nibble 영역).
         * 정상 (not flipped). L=bit 4 (0x10), R=bit 5 (0x20)
         * flip시 (R primary). L=bit 5, R=bit 4 (교환)
         * 케이스 charging은 bit 6 (0x40) — flip 무관.
         */
        const val MASK_DEFAULT_LEFT_CHARGING: Int = 0x10
        const val MASK_DEFAULT_RIGHT_CHARGING: Int = 0x20
        const val MASK_CASE_CHARGING: Int = 0x40

        /** 배터리 4비트 정보 없음 표시값. */
        const val BATTERY_NIBBLE_UNKNOWN: Int = 0x0F

        /** Default 인스턴스. v1.1 기준 (CAPod 정렬). */
        val DEFAULT: ParserConfig = ParserConfig()
    }
}
