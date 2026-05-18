// 메인 화면 UI 상태 — ViewModel이 노출하는 단일 데이터 객체
package com.galaxypods.companion.presentation.main

import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.domain.model.WidgetSnapshot
import com.galaxypods.companion.domain.repository.PodsRepository

/**
 * 메인 화면 UI 상태.
 *
 * Compose 측은 본 객체 하나만 구독해 화면을 그린다 (단일 진실 원천).
 *
 * **lastSnapshot 의미.**
 * AirPods Pro 신펌웨어가 Type 0x07 (Proximity Pairing)을 사실상 송출하지 않아
 * `advertisement`가 null 유지되는 경우가 다수. 이때 마지막으로 받은 스냅샷을
 * "X분 전" 타임스탬프와 함께 표시해 사용자 경험 보전 (PodsLink 패턴).
 *
 * 현재 광고가 있으면 그것 우선, 없으면 lastSnapshot 폴백.
 */
data class MainUiState(
    val status: PodsRepository.ConnectionStatus = PodsRepository.ConnectionStatus.DISCONNECTED,
    val advertisement: AirPodsAdvertisement? = null,
    val lastSnapshot: WidgetSnapshot? = null,
    val autoPauseEnabled: Boolean = true,
    val voiceAnnouncerEnabled: Boolean = false,
)
