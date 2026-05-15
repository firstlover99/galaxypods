// 마지막 위치 화면 ViewModel — DataStore 구독 + 삭제
package com.galaxypods.companion.presentation.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxypods.companion.data.preferences.AppPreferences
import com.galaxypods.companion.domain.model.LastLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 마지막 위치 화면 ViewModel.
 *
 * **책임.**
 * - DataStore의 `lastLocation` Flow 구독 → StateFlow로 노출
 * - 사용자가 "삭제" 버튼 누르면 영속화된 위치 제거
 *
 * **권한 책임 분리.** 본 화면은 위치 권한 요청 X. 권한은 OnboardingScreen 또는
 * 메인 설정에서 사전 처리. 권한 없으면 LastLocationStore가 silent skip → 본 화면은
 * "기록 없음" 표시.
 */
@HiltViewModel
class LastLocationViewModel @Inject constructor(
    private val preferences: AppPreferences,
) : ViewModel() {

    val lastLocation: StateFlow<LastLocation?> = preferences.lastLocation
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    fun clear() {
        viewModelScope.launch { preferences.clearLastLocation() }
    }
}
