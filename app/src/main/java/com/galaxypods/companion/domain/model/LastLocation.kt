// 마지막 위치 도메인 모델 — BLE 끊김 시점에 1회 fetch한 GPS 좌표
package com.galaxypods.companion.domain.model

/**
 * 무선 이어폰 연결이 끊어진 시점의 단말 위치.
 *
 * **수집 정책 (개인정보처리방침 §2.1).**
 * - 사용자 옵트인 (기본 OFF)
 * - 백그라운드 추적 X — 끊김 이벤트마다 1회 fetch
 * - 단말 내에만 저장 (DataStore), 외부 전송 X
 */
data class LastLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracyMeters: Float? = null,
)
