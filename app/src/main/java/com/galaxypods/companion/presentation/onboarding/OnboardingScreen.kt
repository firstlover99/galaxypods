// 온보딩 마법사 Compose UI — 5단계 + 비-삼성 단말 단계 건너뛰기
package com.galaxypods.companion.presentation.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.galaxypods.companion.data.system.SamsungQuirks

/**
 * 온보딩 마법사 진입점.
 *
 * @param onCompleted 모든 단계 완료 시 호출 (MainActivity가 MainScreen으로 전환).
 */
@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding() // 시스템 바(상/하)·디스플레이 컷아웃 회피
                .padding(24.dp),
    ) {
        StepProgress(step = state.step)
        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (state.step) {
                OnboardingStep.WELCOME -> WelcomeStep()
                OnboardingStep.BLUETOOTH_PERMISSION ->
                    BluetoothStep(
                        granted = state.bluetoothGranted,
                        onResult = viewModel::onBluetoothResult,
                    )
                OnboardingStep.NOTIFICATION_PERMISSION ->
                    NotificationStep(
                        granted = state.notificationGranted,
                        onResult = viewModel::onNotificationResult,
                    )
                OnboardingStep.SAMSUNG_BATTERY ->
                    SamsungBatteryStep(
                        oneUiMajorVersion = state.oneUiMajorVersion,
                        isIgnoring = state.ignoringBatteryOptimizations,
                        sleepStatus = state.sleepStatus,
                        onOpenSettings = viewModel::openSamsungBatterySettings,
                    )
                OnboardingStep.LOCATION_OPTIONAL ->
                    LocationStep(
                        locationOptIn = state.locationOptIn,
                        locationGranted = state.locationGranted,
                        onOptInChange = viewModel::setLocationOptIn,
                        onResult = viewModel::onLocationResult,
                    )
                OnboardingStep.DONE ->
                    DoneStep(onCompleted = {
                        viewModel.completeOnboarding(onCompleted)
                    })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        NavigationRow(
            step = state.step,
            onPrevious = viewModel::goPrevious,
            onNext = viewModel::goNext,
        )
    }
}

@Composable
private fun StepProgress(step: OnboardingStep) {
    val total = OnboardingStep.entries.size - 1 // DONE 제외
    val current = step.ordinal.coerceAtMost(total)
    Column {
        LinearProgressIndicator(
            progress = { current.toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "단계 ${current.coerceAtLeast(1)} / $total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NavigationRow(
    step: OnboardingStep,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        OutlinedButton(
            onClick = onPrevious,
            enabled = step != OnboardingStep.WELCOME && step != OnboardingStep.DONE,
        ) {
            Text("이전")
        }
        if (step != OnboardingStep.DONE) {
            Button(onClick = onNext) {
                Text(if (step == OnboardingStep.LOCATION_OPTIONAL) "완료로 이동" else "다음")
            }
        }
    }
}

// ============================================================
// Step 1. Welcome
// ============================================================

@Composable
private fun WelcomeStep() {
    StepContent(
        title = "GalaxyPods에 오신 걸 환영합니다",
        body =
            listOf(
                "Galaxy 단말에서 무선 이어폰을 더 편하게 쓸 수 있도록 5단계 설정을 안내합니다.",
                "권한은 모두 사용자가 직접 선택하며, 언제든 끌 수 있습니다.",
                "본 앱은 사용자 데이터를 외부 서버로 전송하지 않습니다.",
            ),
    )
}

// ============================================================
// Step 2. Bluetooth
// ============================================================

@Composable
private fun BluetoothStep(
    granted: Boolean,
    onResult: (Boolean) -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            onResult(results.values.all { it })
        }

    StepContent(
        title = "Bluetooth 권한",
        body =
            listOf(
                "이어폰의 신호를 받기 위해 Bluetooth 권한이 필요합니다.",
                "위치 정보 사용 안 함(neverForLocation) 플래그가 부착되어, " +
                    "Bluetooth 스캔으로 위치를 추정할 수 없습니다.",
            ),
        action =
            if (granted) {
                { Text("✅ 허용됨", style = MaterialTheme.typography.titleMedium) }
            } else {
                {
                    Button(onClick = {
                        val perms =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                )
                            } else {
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        launcher.launch(perms)
                    }) { Text("권한 요청") }
                }
            },
    )
}

// ============================================================
// Step 3. Notifications
// ============================================================

@Composable
private fun NotificationStep(
    granted: Boolean,
    onResult: (Boolean) -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { onResult(it) }

    StepContent(
        title = "알림 권한",
        body =
            listOf(
                "케이스 오픈 알림과 배터리 경고를 표시하려면 알림 권한이 필요합니다.",
                "광고성 알림은 보내지 않습니다.",
            ),
        action =
            if (granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                { Text("✅ 허용됨 (또는 자동 허용)", style = MaterialTheme.typography.titleMedium) }
            } else {
                {
                    Button(onClick = {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) { Text("권한 요청") }
                }
            },
    )
}

// ============================================================
// Step 4. Samsung Battery (One UI 분기)
// ============================================================

@Composable
private fun SamsungBatteryStep(
    oneUiMajorVersion: Int?,
    isIgnoring: Boolean,
    sleepStatus: SamsungQuirks.SleepStatus,
    onOpenSettings: () -> Unit,
) {
    val guidance =
        when {
            oneUiMajorVersion == null -> "Samsung 단말이 아닙니다. 이 단계를 건너뛰셔도 됩니다."
            oneUiMajorVersion >= 6 ->
                "One UI $oneUiMajorVersion 단말입니다. " +
                    "아래 버튼으로 절전 예외 화면이 자동으로 열립니다. " +
                    "GalaxyPods를 \"절전 안 함\"으로 등록해 주세요."
            oneUiMajorVersion == 5 ->
                "One UI 5 단말입니다. 설정 → 디바이스 케어 → 배터리 → " +
                    "백그라운드 사용 한도 → 절전 안 함 앱 → GalaxyPods 추가."
            else ->
                "One UI ${oneUiMajorVersion ?: "?"} 단말입니다. " +
                    "설정 → 디바이스 관리 → 배터리에서 절전 예외에 GalaxyPods를 추가해 주세요."
        }
    val sleepNote =
        when (sleepStatus) {
            SamsungQuirks.SleepStatus.SLEEPING -> "⚠️ 현재 앱이 SLEEPING 버킷에 있습니다. 절전 예외 등록 권장."
            SamsungQuirks.SleepStatus.NORMAL -> "✓ 앱이 정상 버킷에 있습니다."
            SamsungQuirks.SleepStatus.ACTIVE -> "✓ 앱이 ACTIVE 상태입니다."
            SamsungQuirks.SleepStatus.UNKNOWN -> ""
        }
    val ignoringNote = if (isIgnoring) "✓ 배터리 최적화 예외 등록됨" else "⚠️ 배터리 최적화 예외 미등록"

    StepContent(
        title = "Samsung 절전 정책",
        body = listOf(guidance, sleepNote, ignoringNote).filter { it.isNotBlank() },
        action = {
            Button(onClick = onOpenSettings) {
                Text("절전 설정 열기")
            }
        },
    )
}

// ============================================================
// Step 5. Location (Optional)
// ============================================================

@Composable
private fun LocationStep(
    locationOptIn: Boolean,
    locationGranted: Boolean,
    onOptInChange: (Boolean) -> Unit,
    onResult: (Boolean) -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { onResult(it) }

    StepContent(
        title = "마지막 위치 기록 (선택)",
        body =
            listOf(
                "이어폰과의 연결이 끊긴 마지막 위치를 한 번만 기록해 분실 방지를 돕습니다.",
                "백그라운드에서 위치를 지속적으로 추적하지 않으며, 단말 안에만 저장됩니다.",
                "이 기능은 켜지 않아도 다른 모든 기능이 정상 동작합니다.",
            ),
        action = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("위치 기록 사용", modifier = Modifier.weight(1f))
                    Switch(checked = locationOptIn, onCheckedChange = onOptInChange)
                }
                if (locationOptIn && !locationGranted) {
                    Button(onClick = {
                        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }) { Text("위치 권한 요청") }
                }
            }
        },
    )
}

// ============================================================
// Step 6. Done
// ============================================================

@Composable
private fun DoneStep(onCompleted: () -> Unit) {
    StepContent(
        title = "준비 완료",
        body =
            listOf(
                "이제 AirPods 케이스를 열어보세요. 배터리 정보가 자동으로 표시됩니다.",
                "설정은 메인 화면 우상단에서 언제든 변경할 수 있습니다.",
            ),
        action = {
            Button(onClick = onCompleted) {
                Text("시작하기")
            }
        },
    )
}

// ============================================================
// 공통 단계 레이아웃
// ============================================================

@Composable
private fun StepContent(
    title: String,
    body: List<String>,
    action: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            body.forEach {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (action != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    action()
                }
            }
        }
    }
}
