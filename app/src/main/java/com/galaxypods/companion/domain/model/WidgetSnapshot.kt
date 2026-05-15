// 위젯 / 알림바 표시용 광고 스냅샷 — 영속화 가능한 최소 필드
package com.galaxypods.companion.domain.model

/**
 * 위젯 갱신과 알림바 동적 아이콘에 필요한 최소 데이터.
 *
 * `AirPodsAdvertisement` 전체를 영속화하지 않는 이유.
 * - in-ear / charging 등은 stale일 때 의미 없음 (오해 유발)
 * - lidOpenCount 같은 누적 카운터도 위젯에 불필요
 *
 * 따라서 모델/배터리/timestamp만 영속화 → 시스템 재시작/위젯 추가 시 마지막 값 표시.
 */
data class WidgetSnapshot(
    val model: AirPodsModel,
    val leftBatteryPercent: Int,
    val rightBatteryPercent: Int,
    val caseBatteryPercent: Int,
    val timestamp: Long,
) {
    companion object {
        fun fromAdvertisement(ad: AirPodsAdvertisement): WidgetSnapshot =
            WidgetSnapshot(
                model = ad.model,
                leftBatteryPercent = ad.leftBatteryPercent,
                rightBatteryPercent = ad.rightBatteryPercent,
                caseBatteryPercent = ad.caseBatteryPercent,
                timestamp = ad.timestamp.takeIf { it > 0 } ?: System.currentTimeMillis(),
            )
    }
}
