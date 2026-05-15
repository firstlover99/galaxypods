// 온보딩 마법사 상태 정의 — 단계 enum + 단계별 정보
package com.galaxypods.companion.presentation.onboarding

import com.galaxypods.companion.data.system.SamsungQuirks

/**
 * 온보딩 단계.
 *
 * 검토안 §6.4 5단계 마법사 + Phase 4 결정에 따라 오버레이 권한은 v1.1로 보류.
 * v1.0 흐름: 환영 → BLE → 알림 → Samsung 절전 → 위치(선택) → 완료.
 */
enum class OnboardingStep {
    WELCOME,
    BLUETOOTH_PERMISSION,
    NOTIFICATION_PERMISSION,
    SAMSUNG_BATTERY,
    LOCATION_OPTIONAL,
    DONE,
    ;

    fun next(): OnboardingStep = entries.getOrNull(ordinal + 1) ?: DONE

    fun previous(): OnboardingStep = entries.getOrNull((ordinal - 1).coerceAtLeast(0)) ?: WELCOME
}

/** 온보딩 화면 UI 상태. */
data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val isSamsung: Boolean = false,
    val oneUiMajorVersion: Int? = null,
    val sleepStatus: SamsungQuirks.SleepStatus = SamsungQuirks.SleepStatus.UNKNOWN,
    val ignoringBatteryOptimizations: Boolean = false,
    val bluetoothGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val locationGranted: Boolean = false,
    val locationOptIn: Boolean = false,
)
