// Device Type 2바이트 → AirPods/Beats 모델 매핑 룩업 테이블
package com.galaxypods.companion.data.ble

import com.galaxypods.companion.domain.model.AirPodsModel

/**
 * Continuity 광고의 Device Type 필드(2바이트, little-endian)를 모델 enum으로 매핑한다.
 *
 * **출처.** CAPod (`d4rken-org/capod`) 모델별 DEVICE_CODE + PoPETs 2020 + LibrePods AAP
 * Definitions. 코드 직접 복사가 아닌 식별자 매핑만 보유. 추후 `assets/airpods_keys.json`으로
 * 분리해 데이터-코드 라이선스 격리 강화 예정 (context-notes 결정 5).
 *
 * **바이트 순서.** 와이어 바이트가 `[low, high]` (LE)로 송신되므로 본 테이블의 키도
 * LE 표현. CAPod의 `DEVICE_CODE = 0xLLHH.toUShort()`을 LE로 변환 시 `0xHHLL`.
 * 예. AirPods Pro 2 USB-C는 wire `24 20` → LE 0x2024 (CAPod은 0x2420 BE 표기).
 *
 * **검증 상태 — 2026-05-18 CAPod 정렬.**
 * - ✓. 모든 키를 CAPod 소스 (`AirPodsXxx.kt` `private val DEVICE_CODE`) 검증
 * - ❗ 이전 BeatsSolo3 (0x2003) / BeatsFlex (0x200D) / BeatsStudioBudsPlus (0x201E) /
 *      Powerbeats3 (0x2005) 키 오류 수정
 *
 * 미식별 Device Type은 `UNKNOWN`으로 폴백. 새 모델 출시 시 본 테이블에 추가.
 */
object AirPodsModelTable {
    private val table: Map<Int, AirPodsModel> =
        mapOf(
            // ============================================================
            // AirPods 시리즈
            // ============================================================
            0x2002 to AirPodsModel.AIRPODS_1,
            0x200F to AirPodsModel.AIRPODS_2,
            0x2013 to AirPodsModel.AIRPODS_3,
            0x2019 to AirPodsModel.AIRPODS_4,
            0x201B to AirPodsModel.AIRPODS_4_ANC,
            0x200E to AirPodsModel.AIRPODS_PRO_1,
            0x2014 to AirPodsModel.AIRPODS_PRO_2_LIGHTNING,
            0x2024 to AirPodsModel.AIRPODS_PRO_2_USBC,
            0x2027 to AirPodsModel.AIRPODS_PRO_3,
            0x200A to AirPodsModel.AIRPODS_MAX_LIGHTNING,
            0x201F to AirPodsModel.AIRPODS_MAX_USBC,
            0x202D to AirPodsModel.AIRPODS_MAX_2,
            // ============================================================
            // Beats 시리즈 (Apple 인수, 동일 0x004C 광고 포맷)
            // ============================================================
            0x2005 to AirPodsModel.BEATS_X,
            0x2003 to AirPodsModel.POWERBEATS_3,
            0x200D to AirPodsModel.POWERBEATS_4,
            0x200B to AirPodsModel.POWERBEATS_PRO,
            0x201D to AirPodsModel.POWERBEATS_PRO_2,
            0x2006 to AirPodsModel.BEATS_SOLO_3,
            0x200C to AirPodsModel.BEATS_SOLO_PRO,
            0x2025 to AirPodsModel.BEATS_SOLO_4,
            0x2026 to AirPodsModel.BEATS_SOLO_BUDS,
            0x2009 to AirPodsModel.BEATS_STUDIO_3,
            0x2017 to AirPodsModel.BEATS_STUDIO_PRO,
            0x2011 to AirPodsModel.BEATS_STUDIO_BUDS,
            0x2016 to AirPodsModel.BEATS_STUDIO_BUDS_PLUS,
            0x2012 to AirPodsModel.BEATS_FIT_PRO,
            0x2010 to AirPodsModel.BEATS_FLEX,
        )

    /**
     * Device Type 2바이트(int 표현)로 모델 식별.
     * 미식별 시 `AirPodsModel.UNKNOWN` 반환.
     */
    fun identify(deviceType: Int): AirPodsModel = table[deviceType] ?: AirPodsModel.UNKNOWN

    /** 현재 지원 모델 총 개수 (검증된 키 + 추정 키). */
    val supportedCount: Int get() = table.size
}
