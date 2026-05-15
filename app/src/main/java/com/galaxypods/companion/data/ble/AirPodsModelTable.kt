// Device Type 2바이트 → AirPods/Beats 모델 매핑 룩업 테이블
package com.galaxypods.companion.data.ble

import com.galaxypods.companion.domain.model.AirPodsModel

/**
 * Continuity 광고의 Device Type 필드(2바이트, big-endian)를 모델 enum으로 매핑한다.
 *
 * **출처.** CAPod Wiki "AirPod Keys" + LibrePods AAP Definitions + Apple 펌웨어 산물(facts).
 * 코드 직접 복사가 아닌 식별자 매핑만 보유. 추후 `assets/airpods_keys.json`으로
 * 분리해 데이터-코드 라이선스 격리 강화 예정 (context-notes 결정 5).
 *
 * **검증 상태.**
 * - ✓. AirPods 2/3/Pro 1/Pro 2 (Lightning/USB-C) - 다수 OSS 합의
 * - ◯. AirPods 4 / 4 ANC / Pro 3 / Max - 출시 후 OSS 합의 (실측 재검증 필요)
 * - ◯. Beats 시리즈 - PodsLink 등 경쟁 앱이 지원, 키는 OSS 합의
 *
 * 미식별 Device Type은 `UNKNOWN`으로 폴백. 새 모델 출시 시 본 테이블에 추가.
 */
object AirPodsModelTable {

    private val table: Map<Int, AirPodsModel> = mapOf(
        // ============================================================
        // AirPods 시리즈
        // ============================================================
        0x200F to AirPodsModel.AIRPODS_2,
        0x2013 to AirPodsModel.AIRPODS_3,
        0x2019 to AirPodsModel.AIRPODS_4,
        0x201B to AirPodsModel.AIRPODS_4_ANC,
        0x200E to AirPodsModel.AIRPODS_PRO_1,
        0x2014 to AirPodsModel.AIRPODS_PRO_2_LIGHTNING,
        0x2024 to AirPodsModel.AIRPODS_PRO_2_USBC,
        // AIRPODS_PRO_3 키는 실측 dump 확보 후 추가
        0x200A to AirPodsModel.AIRPODS_MAX_LIGHTNING,
        // AIRPODS_MAX_USBC 키는 실측 dump 확보 후 추가

        // ============================================================
        // Beats 시리즈 (Apple 인수, 동일 0x004C 광고 포맷)
        // ============================================================
        0x2005 to AirPodsModel.POWERBEATS_3,
        0x200B to AirPodsModel.POWERBEATS_PRO,
        0x2003 to AirPodsModel.BEATS_SOLO_3,
        0x200C to AirPodsModel.BEATS_SOLO_PRO,
        0x2025 to AirPodsModel.BEATS_SOLO_4,
        0x2009 to AirPodsModel.BEATS_STUDIO_3,
        0x2017 to AirPodsModel.BEATS_STUDIO_PRO,
        0x2011 to AirPodsModel.BEATS_STUDIO_BUDS,
        0x201E to AirPodsModel.BEATS_STUDIO_BUDS_PLUS,
        0x2012 to AirPodsModel.BEATS_FIT_PRO,
        0x200D to AirPodsModel.BEATS_FLEX,
    )

    /**
     * Device Type 2바이트(int 표현)로 모델 식별.
     * 미식별 시 `AirPodsModel.UNKNOWN` 반환.
     */
    fun identify(deviceType: Int): AirPodsModel = table[deviceType] ?: AirPodsModel.UNKNOWN

    /** 현재 지원 모델 총 개수 (검증된 키 + 추정 키). */
    val supportedCount: Int get() = table.size
}
