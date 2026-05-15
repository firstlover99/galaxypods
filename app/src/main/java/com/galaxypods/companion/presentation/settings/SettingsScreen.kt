// 설정 화면 — 모든 토글 + 임계값 + Samsung 절전 + Crashlytics 옵트인
package com.galaxypods.companion.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.galaxypods.companion.domain.usecase.AutoPlayPause

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_revert),
                            contentDescription = "뒤로",
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle("귀감지 자동 정지")
            SettingCard {
                SwitchRow(
                    title = "자동 정지/재생 사용",
                    checked = state.autoPauseEnabled,
                    onCheckedChange = viewModel::setAutoPauseEnabled,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "정지 트리거 모드",
                    style = MaterialTheme.typography.titleSmall,
                )
                ModeRadio(
                    label = "한쪽이라도 빼면 정지 (권장)",
                    selected = state.autoPauseMode == AutoPlayPause.Mode.RELAXED_EITHER,
                    onClick = { viewModel.setAutoPauseMode(AutoPlayPause.Mode.RELAXED_EITHER) },
                )
                ModeRadio(
                    label = "양쪽 모두 빼야 정지",
                    selected = state.autoPauseMode == AutoPlayPause.Mode.STRICT_BOTH,
                    onClick = { viewModel.setAutoPauseMode(AutoPlayPause.Mode.STRICT_BOTH) },
                )
            }

            SectionTitle("음성 안내")
            SettingCard {
                SwitchRow(
                    title = "한국어 음성으로 배터리 안내",
                    checked = state.voiceEnabled,
                    onCheckedChange = viewModel::setVoiceEnabled,
                )
                if (state.voiceEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "안내 임계값",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    listOf(10, 20, 30).forEach { threshold ->
                        ModeRadio(
                            label = "$threshold% 이하일 때 안내",
                            selected = state.voiceThreshold == threshold,
                            onClick = { viewModel.setVoiceThreshold(threshold) },
                        )
                    }
                }
            }

            SectionTitle("위치 / 분실 방지")
            SettingCard {
                SwitchRow(
                    title = "마지막 위치 기록 (선택)",
                    checked = state.locationEnabled,
                    onCheckedChange = viewModel::setLocationEnabled,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SwitchRow(
                    title = "케이스 분실 알림 (5분/50m)",
                    checked = state.caseLostEnabled,
                    onCheckedChange = viewModel::setCaseLostEnabled,
                )
                Text(
                    text = "위치 기록을 켜야 분실 알림도 동작합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.isSamsung) {
                SectionTitle("Samsung 절전")
                SettingCard {
                    Text(
                        text =
                            if (state.ignoringBatteryOptimizations) {
                                "✓ 배터리 최적화 예외에 등록되어 있습니다."
                            } else {
                                "⚠️ 배터리 최적화 예외에 등록되지 않았습니다. 백그라운드 동작이 끊길 수 있어요."
                            },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::openSamsungBatterySettings,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("절전 설정 열기")
                    }
                }
            }

            SectionTitle("개선 도움 (선택)")
            SettingCard {
                SwitchRow(
                    title = "익명 충돌 보고서 보내기 (Crashlytics)",
                    checked = state.crashlyticsOptIn,
                    onCheckedChange = viewModel::setCrashlyticsOptIn,
                )
                Text(
                    text = "위치, 페어링 이력, 식별자는 절대 포함되지 않으며 언제든 끌 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ModeRadio(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, onClick = onClick)
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.padding(start = 8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
