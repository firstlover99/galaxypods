// AirPods BLE 광고 파싱 결과 — 모든 상태 필드를 한 번에 표현
package com.galaxypods.companion.domain.model

/**
 * AirPods Continuity Proximity Pairing 광고 한 프레임의 파싱 결과.
 *
 * 배터리는 광고 평문에서 4비트(0~15)로 인코딩되며 본 모델은 0~100 (10% 단위)로 정규화한다.
 * 0xF (15)는 "정보 없음(unknown)" → -1 로 표현.
 *
 * `lidOpenCount`는 케이스 오픈 카운터. 변화량으로 "방금 열렸다"를 감지한다.
 *
 * IRK 회전으로 BD_ADDR이 무작위 변경되므로 동일 기기 추적은 본 모델 외부의
 * 휴리스틱(lidOpenCount + 배터리 패턴 + RSSI 안정성)에서 처리한다.
 */
data class AirPodsAdvertisement(
    val model: AirPodsModel,
    val leftBatteryPercent: Int,
    val rightBatteryPercent: Int,
    val caseBatteryPercent: Int,
    val leftInEar: Boolean,
    val rightInEar: Boolean,
    val leftCharging: Boolean,
    val rightCharging: Boolean,
    val caseCharging: Boolean,
    val lidOpenCount: Int,
    val rssi: Int = 0,
    val timestamp: Long = 0L,
) {
    companion object {
        const val BATTERY_UNKNOWN: Int = -1
    }
}
