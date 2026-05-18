// AirPods BLE 광고 파싱 결과 — 모든 상태 필드를 한 번에 표현
package com.galaxypods.companion.domain.model

/**
 * AirPods Continuity Proximity Pairing 광고 한 프레임의 파싱 결과.
 *
 * 배터리는 광고 평문에서 4비트(0~15)로 인코딩되며 본 모델은 0~100 (10% 단위)로 정규화한다.
 * 0xF (15)는 "정보 없음(unknown)" → -1 로 표현. 11~14는 "above 100%" → 100으로 캡.
 *
 * `lidOpenCount`는 케이스 오픈 카운터. 변화량으로 "방금 열렸다"를 감지한다.
 * `lidState`는 lid byte bit 3 + case 컨텍스트로 OPEN/CLOSED/NOT_IN_CASE 디코딩 (CAPod 정렬).
 *
 * **CAPod 정렬 신규 필드 (2026-05-18).**
 * - `lidState`. 케이스 뚜껑 상태
 * - `isThisPodInCase`, `isOnePodInCase`, `areBothPodsInCase`. 페어 위치
 * - `isLeftPodPrimary`. 마이크 송신 활성 pod (= L primary)
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
    /** 케이스 뚜껑 상태. CAPod LidState 정렬. */
    val lidState: LidState = LidState.NOT_IN_CASE,
    /** 본 광고를 송신한 pod (broadcasting pod)이 케이스에 있는지. */
    val isThisPodInCase: Boolean = false,
    /** 한 쪽 pod가 케이스에 있는지 (반대쪽은 귀에/밖에 있음). */
    val isOnePodInCase: Boolean = false,
    /** 양쪽 pod 모두 케이스에 있는지. */
    val areBothPodsInCase: Boolean = false,
    /** 마이크/통신 활성 pod이 L인지 (true=L primary, false=R primary). */
    val isLeftPodPrimary: Boolean = true,
    val rssi: Int = 0,
    val timestamp: Long = 0L,
) {
    /** UI helper. 케이스 컨텍스트 있는지 (3개 위치 비트 중 하나라도). */
    val hasCaseContext: Boolean
        get() = isThisPodInCase || isOnePodInCase || areBothPodsInCase

    companion object {
        const val BATTERY_UNKNOWN: Int = -1
    }
}
