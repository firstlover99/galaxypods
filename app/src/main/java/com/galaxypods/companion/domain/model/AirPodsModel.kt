// Apple 0x004C 광고를 송출하는 무선 이어폰 모델 enum (AirPods + Beats)
package com.galaxypods.companion.domain.model

/**
 * Apple Continuity Proximity Pairing 광고를 송출하는 모든 호환 모델.
 *
 * AirPods 시리즈 외에 Beats 시리즈 (Apple 인수)도 동일한 0x004C / Type 0x07 광고
 * 포맷을 사용하므로 같은 파서로 처리한다. PodsLink 등 경쟁 앱이 Beats를 지원하는
 * 핵심 이유.
 *
 * `displayName`은 UI 표시용. `hasAnc`는 §3 기능 분기에 사용.
 * `category`로 AirPods / Beats / 기타 분류.
 *
 * **CAPod (d4rken-org/capod) 모델 코드 정렬 — 2026-05-18.**
 * 기존 테이블의 BeatsSolo3 / BeatsFlex / BeatsStudioBudsPlus / Powerbeats3 키 오류
 * 수정 + AirPods 1세대, AirPods Max USB-C / Max 2, BeatsX / PowerBeats4 / PowerBeats Pro 2
 * / Beats Solo Buds 신규 추가.
 */
enum class AirPodsModel(
    val displayName: String,
    val hasAnc: Boolean,
    val category: Category,
) {
    // ============================================================
    // AirPods 시리즈
    // ============================================================
    AIRPODS_1("AirPods 1세대", false, Category.AIRPODS),
    AIRPODS_2("AirPods 2세대", false, Category.AIRPODS),
    AIRPODS_3("AirPods 3세대", false, Category.AIRPODS),
    AIRPODS_4("AirPods 4", false, Category.AIRPODS),
    AIRPODS_4_ANC("AirPods 4 (ANC)", true, Category.AIRPODS),
    AIRPODS_PRO_1("AirPods Pro 1세대", true, Category.AIRPODS),
    AIRPODS_PRO_2_LIGHTNING("AirPods Pro 2세대", true, Category.AIRPODS),
    AIRPODS_PRO_2_USBC("AirPods Pro 2 (USB-C)", true, Category.AIRPODS),
    AIRPODS_PRO_3("AirPods Pro 3세대", true, Category.AIRPODS),
    AIRPODS_MAX_LIGHTNING("AirPods Max", true, Category.AIRPODS),
    AIRPODS_MAX_USBC("AirPods Max (USB-C)", true, Category.AIRPODS),
    AIRPODS_MAX_2("AirPods Max 2세대", true, Category.AIRPODS),

    // ============================================================
    // Beats 시리즈 (Apple 인수, 동일 광고 포맷 사용)
    // ============================================================
    BEATS_X("BeatsX", false, Category.BEATS),
    POWERBEATS_3("Powerbeats 3", false, Category.BEATS),
    POWERBEATS_4("Powerbeats 4", false, Category.BEATS),
    POWERBEATS_PRO("Powerbeats Pro", false, Category.BEATS),
    POWERBEATS_PRO_2("Powerbeats Pro 2", true, Category.BEATS),
    BEATS_SOLO_3("Beats Solo 3", false, Category.BEATS),
    BEATS_SOLO_PRO("Beats Solo Pro", true, Category.BEATS),
    BEATS_SOLO_4("Beats Solo 4", false, Category.BEATS),
    BEATS_SOLO_BUDS("Beats Solo Buds", false, Category.BEATS),
    BEATS_STUDIO_3("Beats Studio 3", true, Category.BEATS),
    BEATS_STUDIO_PRO("Beats Studio Pro", true, Category.BEATS),
    BEATS_STUDIO_BUDS("Beats Studio Buds", true, Category.BEATS),
    BEATS_STUDIO_BUDS_PLUS("Beats Studio Buds+", true, Category.BEATS),
    BEATS_FIT_PRO("Beats Fit Pro", true, Category.BEATS),
    BEATS_FLEX("Beats Flex", false, Category.BEATS),

    // ============================================================
    // 폴백
    // ============================================================
    UNKNOWN("알 수 없는 기기", false, Category.UNKNOWN),
    ;

    /** 모델 분류. UI 그룹화 / 아이콘 폴백 / 통계용. */
    enum class Category { AIRPODS, BEATS, UNKNOWN }
}
