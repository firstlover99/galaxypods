// 메인 화면 UI 상태 — ViewModel이 노출하는 단일 데이터 객체
package com.galaxypods.companion.presentation.main

import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.domain.repository.PodsRepository

/**
 * 메인 화면 UI 상태.
 *
 * Compose 측은 본 객체 하나만 구독해 화면을 그린다 (단일 진실 원천).
 */
data class MainUiState(
    val status: PodsRepository.ConnectionStatus = PodsRepository.ConnectionStatus.DISCONNECTED,
    val advertisement: AirPodsAdvertisement? = null,
    val autoPauseEnabled: Boolean = true,
    val voiceAnnouncerEnabled: Boolean = false,
)
