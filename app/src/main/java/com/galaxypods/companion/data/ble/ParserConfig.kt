// AppleContinuityParser 비트 오프셋 설정 — 펌웨어 변경 시 코드 수정 없이 교체 가능
package com.galaxypods.companion.data.ble

/**
 * Apple Continuity Proximity Pairing 페이로드 내 필드 오프셋.
 *
 * Apple이 펌웨어 업데이트로 필드 위치를 옮길 경우, 코드 변경 없이 본 데이터 클래스만
 * 교체(추후 Firebase Remote Config로 `parser_config.json` 다운로드)하면 동작 복구 가능.
 *
 * **기준 자료.** PoPETs 2020 (Celosia & Cunche) Fig.15 + LibrePods AAP Definitions.md
 * 그리고 설계안 §4.4 코드. 오프셋은 설계안 기준이며 실측 dump로 재검증 필요.
 *
 * **검증.** `app/src/test/resources/ble_dumps/` 골든바이트로 매번 테스트.
 */
data class ParserConfig(
    val version: String = "v1.0-design-doc",
    val proximityPairingType: Int = TYPE_PROXIMITY_PAIRING,
    val expectedLength: Int = LENGTH_PROXIMITY_PAIRING,
    /**
     * Device Type 2바이트 시작 오프셋.
     *
     * Apple Continuity Type 0x07 페이로드 레이아웃.
     * - payload[0] = prefix (보통 0x07)
     * - payload[1..2] = Device Type (**little-endian**) ← 본 오프셋
     *
     * 따라서 offset=1. 읽기는 LE: payload[offset]=low, payload[offset+1]=high.
     *
     * **검증.** 2026-05-18 Galaxy Note 20 USB로 AirPods Pro 2 USB-C 재페어링 시
     * 캡처한 패킷 `07 19 07 24 20 15 ...`에서 wire 바이트 `24 20`을 LE로
     * 읽으면 0x2024 = AIRPODS_PRO_2_USBC 룩업 일치.
     */
    val deviceTypeOffset: Int = 1,
    /** in-ear 비트 플래그가 있는 바이트 오프셋. left=bit3, right=bit1. */
    val inEarOffset: Int = 3,
    /** 배터리 1바이트(상위 4비트=right, 하위 4비트=left, 0~15). */
    val battery1Offset: Int = 5,
    /** 배터리 2바이트(하위 4비트=case, 상위 4비트=예약/충전). */
    val battery2Offset: Int = 6,
    /** 충전 플래그 바이트. bit0=right, bit1=left, bit2=case. */
    val chargingOffset: Int = 7,
    /** 케이스 lid open 카운터(0~255). 변화량으로 케이스 오픈 감지. */
    val lidOpenOffset: Int = 8,
) {
    companion object {
        /** Apple Inc. — Bluetooth SIG 할당 manufacturer ID. */
        const val APPLE_MANUFACTURER_ID: Int = 0x004C

        /** Continuity TLV 타입. AirPods 근접 페어링 광고. */
        const val TYPE_PROXIMITY_PAIRING: Int = 0x07

        /** Type 0x07 페이로드 길이. 0x19 = 25 바이트. */
        const val LENGTH_PROXIMITY_PAIRING: Int = 0x19

        /** in-ear 비트 마스크 (왼쪽). */
        const val MASK_LEFT_IN_EAR: Int = 0x08

        /** in-ear 비트 마스크 (오른쪽). */
        const val MASK_RIGHT_IN_EAR: Int = 0x02

        /** 충전 비트 마스크 (오른쪽). */
        const val MASK_RIGHT_CHARGING: Int = 0x01

        /** 충전 비트 마스크 (왼쪽). */
        const val MASK_LEFT_CHARGING: Int = 0x02

        /** 충전 비트 마스크 (케이스). */
        const val MASK_CASE_CHARGING: Int = 0x04

        /** 배터리 4비트 정보 없음 표시값. */
        const val BATTERY_NIBBLE_UNKNOWN: Int = 0x0F

        /** Default 인스턴스. v1.0 기준. */
        val DEFAULT: ParserConfig = ParserConfig()
    }
}
