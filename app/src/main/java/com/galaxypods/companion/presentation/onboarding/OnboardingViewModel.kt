// 온보딩 마법사 ViewModel — 단계 진행 + 권한 결과 + Samsung 딥링크
package com.galaxypods.companion.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxypods.companion.data.preferences.AppPreferences
import com.galaxypods.companion.data.system.SamsungQuirks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 온보딩 마법사 ViewModel.
 *
 * **책임.**
 * - 단계 진행/후퇴
 * - Activity로부터 권한 결과 통보 받기
 * - Samsung 절전 딥링크 호출 ([SamsungQuirks])
 * - 완료 시 [AppPreferences]에 플래그 영속화
 */
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val samsungQuirks: SamsungQuirks,
        private val preferences: AppPreferences,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(initialState())
        val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

        private fun initialState(): OnboardingUiState =
            OnboardingUiState(
                isSamsung = samsungQuirks.isSamsungDevice,
                oneUiMajorVersion = samsungQuirks.oneUiMajorVersion(),
                sleepStatus = samsungQuirks.sleepStatus(),
                ignoringBatteryOptimizations = samsungQuirks.isIgnoringBatteryOptimizations(),
            )

        fun goNext() {
            val current = _uiState.value.step
            // 비-삼성 단말은 SAMSUNG_BATTERY 단계 건너뜀
            val next =
                if (current == OnboardingStep.NOTIFICATION_PERMISSION && !_uiState.value.isSamsung) {
                    OnboardingStep.LOCATION_OPTIONAL
                } else {
                    current.next()
                }
            _uiState.update { it.copy(step = next) }
        }

        fun goPrevious() {
            val current = _uiState.value.step
            val previous =
                if (current == OnboardingStep.LOCATION_OPTIONAL && !_uiState.value.isSamsung) {
                    OnboardingStep.NOTIFICATION_PERMISSION
                } else {
                    current.previous()
                }
            _uiState.update { it.copy(step = previous) }
        }

        fun onBluetoothResult(granted: Boolean) {
            _uiState.update { it.copy(bluetoothGranted = granted) }
        }

        fun onNotificationResult(granted: Boolean) {
            _uiState.update { it.copy(notificationGranted = granted) }
        }

        fun onLocationResult(granted: Boolean) {
            _uiState.update { it.copy(locationGranted = granted) }
        }

        fun setLocationOptIn(value: Boolean) {
            _uiState.update { it.copy(locationOptIn = value) }
            viewModelScope.launch { preferences.setLocationRecordEnabled(value) }
        }

        /** 사용자가 "Samsung 절전 설정 열기" 버튼을 눌렀을 때. */
        fun openSamsungBatterySettings() {
            samsungQuirks.openBatteryOptimizationSettings()
        }

        /** Activity가 onResume 시 호출 → Samsung 절전 화면에서 돌아왔을 때 상태 갱신. */
        fun refreshSystemStatus() {
            _uiState.update {
                it.copy(
                    sleepStatus = samsungQuirks.sleepStatus(),
                    ignoringBatteryOptimizations = samsungQuirks.isIgnoringBatteryOptimizations(),
                )
            }
        }

        /** 마지막 단계에서 호출. 영속화 후 콜백 통해 Activity가 메인 화면으로 전환. */
        fun completeOnboarding(onCompleted: () -> Unit) {
            viewModelScope.launch {
                preferences.setOnboardingCompleted(true)
                onCompleted()
            }
        }
    }
