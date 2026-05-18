// 메인 화면 ViewModel — Repository 상태 + 토글 + FGS 제어 + 마지막 스냅샷
package com.galaxypods.companion.presentation.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxypods.companion.data.preferences.AppPreferences
import com.galaxypods.companion.data.system.VoiceAnnouncer
import com.galaxypods.companion.domain.repository.PodsRepository
import com.galaxypods.companion.domain.usecase.AutoPlayPause
import com.galaxypods.companion.service.PodsForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * 메인 화면 ViewModel.
 *
 * **책임.**
 * - Repository의 광고/연결 상태를 [MainUiState]로 합쳐 노출
 * - 사용자 토글(귀감지·음성안내)을 UseCase로 위임
 * - FGS 시작/중지를 트리거 (ViewModel은 Service에 직접 의존하지 않고
 *   ApplicationContext만 받음)
 * - DataStore에 저장된 마지막 스냅샷을 결합해 광고 없을 때 폴백 표시 가능하게 함
 *
 * Phase 4에서 `AppPreferences`로 토글 상태가 영속화되면 본 ViewModel은 그쪽으로 위임.
 */
@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
        private val repository: PodsRepository,
        private val autoPlayPause: AutoPlayPause,
        private val voiceAnnouncer: VoiceAnnouncer,
        private val preferences: AppPreferences,
    ) : ViewModel() {
        private val _autoPauseEnabled = MutableStateFlow(autoPlayPause.enabled)
        private val _voiceAnnouncerEnabled = MutableStateFlow(voiceAnnouncer.enabled)

        val uiState: StateFlow<MainUiState> = combineState()

        init {
            // 토글 변화 → UseCase에 즉시 반영
            _autoPauseEnabled
                .onEach {
                    autoPlayPause.enabled = it
                    if (!it) autoPlayPause.reset()
                }
                .launchIn(viewModelScope)

            _voiceAnnouncerEnabled
                .onEach { voiceAnnouncer.enabled = it }
                .launchIn(viewModelScope)
        }

        fun startService() = PodsForegroundService.start(appContext)

        fun stopService() = PodsForegroundService.stop(appContext)

        fun setAutoPause(enabled: Boolean) {
            _autoPauseEnabled.value = enabled
        }

        fun setVoiceAnnouncer(enabled: Boolean) {
            _voiceAnnouncerEnabled.value = enabled
        }

        private fun combineState(): StateFlow<MainUiState> {
            val backing = MutableStateFlow(MainUiState())
            // combine은 인자 5개까지만 직접 지원 → 6개라 vararg 형태 사용
            combine(
                repository.advertisement,
                repository.connectionStatus,
                _autoPauseEnabled,
                _voiceAnnouncerEnabled,
                preferences.widgetSnapshot,
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val ad = values[0] as com.galaxypods.companion.domain.model.AirPodsAdvertisement?

                @Suppress("UNCHECKED_CAST")
                val status = values[1] as PodsRepository.ConnectionStatus

                @Suppress("UNCHECKED_CAST")
                val autoPause = values[2] as Boolean

                @Suppress("UNCHECKED_CAST")
                val voice = values[3] as Boolean

                @Suppress("UNCHECKED_CAST")
                val snapshot = values[4] as com.galaxypods.companion.domain.model.WidgetSnapshot?
                MainUiState(
                    status = status,
                    advertisement = ad,
                    lastSnapshot = snapshot,
                    autoPauseEnabled = autoPause,
                    voiceAnnouncerEnabled = voice,
                )
            }
                .onEach { backing.value = it }
                .launchIn(viewModelScope)
            return backing.asStateFlow()
        }
    }
