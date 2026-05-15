// 메인 액티비티 — 온보딩 / 메인 분기 + Compose 진입점
package com.galaxypods.companion.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxypods.companion.data.preferences.AppPreferences
import com.galaxypods.companion.presentation.main.MainScreen
import com.galaxypods.companion.presentation.onboarding.OnboardingScreen
import com.galaxypods.companion.presentation.theme.GalaxyPodsTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 앱 진입점.
 *
 * **분기.**
 * - 첫 실행 (온보딩 미완료) → [OnboardingScreen]
 * - 이후 → [MainScreen]
 *
 * Onboarding 완료 시 in-memory 토글로 즉시 MainScreen 전환.
 * Activity 재시작 시에는 DataStore의 onboardingCompleted 값으로 결정.
 *
 * 권한 요청은 OnboardingScreen 각 단계에서 처리. 본 액티비티는 권한 요청 없음.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GalaxyPodsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppContent()
                }
            }
        }
    }
}

@Composable
private fun AppContent(viewModel: AppGateViewModel = hiltViewModel()) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
    var localBypass by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (onboardingCompleted || localBypass) {
            MainScreen()
        } else {
            OnboardingScreen(
                onCompleted = {
                    // DataStore 비동기 영속화와 별개로 즉시 메인 화면 전환
                    localBypass = true
                },
            )
        }
    }
}

/**
 * Onboarding 완료 여부를 한 곳에서 관찰하는 게이트 ViewModel.
 *
 * MainActivity가 OnboardingScreen / MainScreen 중 무엇을 띄울지 결정.
 */
@HiltViewModel
class AppGateViewModel
    @Inject
    constructor(
        preferences: AppPreferences,
    ) : ViewModel() {
        val onboardingCompleted: StateFlow<Boolean> =
            preferences.onboardingCompleted
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = false,
                )
    }
