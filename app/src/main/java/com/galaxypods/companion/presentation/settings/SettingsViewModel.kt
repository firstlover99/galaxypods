// 설정 화면 ViewModel — DataStore 토글 + UseCase 모드 변경
package com.galaxypods.companion.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxypods.companion.data.preferences.AppPreferences
import com.galaxypods.companion.data.system.CrashReporter
import com.galaxypods.companion.data.system.SamsungQuirks
import com.galaxypods.companion.data.system.VoiceAnnouncer
import com.galaxypods.companion.domain.usecase.AutoPlayPause
import com.galaxypods.companion.domain.usecase.CaseLostDetect
import com.galaxypods.companion.domain.usecase.CaseOpenDetect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val autoPauseEnabled: Boolean = true,
    val autoPauseMode: AutoPlayPause.Mode = AutoPlayPause.Mode.RELAXED_EITHER,
    val voiceEnabled: Boolean = false,
    val voiceThreshold: Int = VoiceAnnouncer.DEFAULT_THRESHOLD_PERCENT,
    val locationEnabled: Boolean = false,
    val caseLostEnabled: Boolean = false,
    val crashlyticsOptIn: Boolean = false,
    val isSamsung: Boolean = false,
    val ignoringBatteryOptimizations: Boolean = false,
)

/**
 * 설정 화면 ViewModel.
 *
 * AppPreferences 6개 키를 한 화면에서 노출 + UseCase에 즉시 반영.
 * Samsung 절전 예외 등록 화면 직접 열기도 노출.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val preferences: AppPreferences,
        private val autoPlayPause: AutoPlayPause,
        private val voiceAnnouncer: VoiceAnnouncer,
        private val caseLostDetect: CaseLostDetect,
        private val caseOpenDetect: CaseOpenDetect,
        private val crashReporter: CrashReporter,
        private val samsungQuirks: SamsungQuirks,
    ) : ViewModel() {
        val uiState: StateFlow<SettingsUiState> =
            combine(
                preferences.autoPauseEnabled,
                preferences.autoPauseMode,
                preferences.voiceAnnouncerEnabled,
                preferences.voiceAnnouncerThreshold,
                combine(
                    preferences.locationRecordEnabled,
                    preferences.caseLostAlertEnabled,
                    preferences.crashlyticsOptIn,
                ) { loc, lost, crash -> Triple(loc, lost, crash) },
            ) { autoPause, mode, voice, threshold, group ->
                SettingsUiState(
                    autoPauseEnabled = autoPause,
                    autoPauseMode = mode,
                    voiceEnabled = voice,
                    voiceThreshold = threshold,
                    locationEnabled = group.first,
                    caseLostEnabled = group.second,
                    crashlyticsOptIn = group.third,
                    isSamsung = samsungQuirks.isSamsungDevice,
                    ignoringBatteryOptimizations = samsungQuirks.isIgnoringBatteryOptimizations(),
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue =
                    SettingsUiState(
                        isSamsung = samsungQuirks.isSamsungDevice,
                    ),
            )

        fun setAutoPauseEnabled(value: Boolean) =
            viewModelScope.launch {
                preferences.setAutoPauseEnabled(value)
                autoPlayPause.enabled = value
                if (!value) autoPlayPause.reset()
            }

        fun setAutoPauseMode(mode: AutoPlayPause.Mode) =
            viewModelScope.launch {
                preferences.setAutoPauseMode(mode)
                autoPlayPause.mode = mode
            }

        fun setVoiceEnabled(value: Boolean) =
            viewModelScope.launch {
                preferences.setVoiceAnnouncerEnabled(value)
                voiceAnnouncer.enabled = value
            }

        fun setVoiceThreshold(value: Int) =
            viewModelScope.launch {
                preferences.setVoiceAnnouncerThreshold(value)
                voiceAnnouncer.thresholdPercent = value
            }

        fun setLocationEnabled(value: Boolean) =
            viewModelScope.launch {
                preferences.setLocationRecordEnabled(value)
            }

        fun setCaseLostEnabled(value: Boolean) =
            viewModelScope.launch {
                preferences.setCaseLostAlertEnabled(value)
                caseLostDetect.enabled = value
            }

        fun setCaseOpenEnabled(value: Boolean) {
            caseOpenDetect.enabled = value
        }

        fun setCrashlyticsOptIn(value: Boolean) =
            viewModelScope.launch {
                preferences.setCrashlyticsOptIn(value)
                crashReporter.setEnabled(value)
            }

        fun openSamsungBatterySettings() {
            samsungQuirks.openBatteryOptimizationSettings()
        }
    }
