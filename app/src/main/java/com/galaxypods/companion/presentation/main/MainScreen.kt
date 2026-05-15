// 메인 화면 Compose UI — 검토안 §6.2 + Tip 카드 + Settings 진입
package com.galaxypods.companion.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.galaxypods.companion.R
import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.domain.repository.PodsRepository
import com.galaxypods.companion.domain.tip.Tip
import com.galaxypods.companion.domain.tip.TipProvider
import com.galaxypods.companion.presentation.about.AboutScreen
import com.galaxypods.companion.presentation.location.LastLocationScreen
import com.galaxypods.companion.presentation.settings.SettingsScreen

private enum class Route { HOME, SETTINGS, LOCATION, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
) {
    var route by remember { mutableStateOf(Route.HOME) }

    when (route) {
        Route.HOME -> HomeContent(
            viewModel = viewModel,
            onSettings = { route = Route.SETTINGS },
            onLocation = { route = Route.LOCATION },
            onAbout = { route = Route.ABOUT },
        )
        Route.SETTINGS -> SettingsScreen(onBack = { route = Route.HOME })
        Route.LOCATION -> LastLocationScreen(onBack = { route = Route.HOME })
        Route.ABOUT -> AboutScreen(onBack = { route = Route.HOME })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    viewModel: MainViewModel,
    onSettings: () -> Unit,
    onLocation: () -> Unit,
    onAbout: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val tip = remember { TipProvider().tipOfTheDay() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onAbout) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_info_details),
                            contentDescription = "정보",
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_preferences),
                            contentDescription = "설정",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ConnectionHeader(state = state)
            Spacer(modifier = Modifier.height(8.dp))

            BatteryRow(advertisement = state.advertisement)

            ToggleRow(
                title = "👂 귀감지 자동 정지",
                checked = state.autoPauseEnabled,
                onCheckedChange = viewModel::setAutoPause,
            )
            ToggleRow(
                title = "🔊 한국어 음성 안내",
                checked = state.voiceAnnouncerEnabled,
                onCheckedChange = viewModel::setVoiceAnnouncer,
            )

            ActionRow(
                title = "📍 마지막 위치 보기",
                onClick = onLocation,
            )

            TipCard(tip = tip)

            DisclaimerSection()
        }
    }
}

@Composable
private fun ConnectionHeader(state: MainUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = state.advertisement?.model?.displayName?.firstOrNull()?.toString() ?: "—",
                fontSize = 36.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = state.advertisement?.model?.displayName ?: "이어폰을 찾는 중",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = state.status.toUserFacing(),
            style = MaterialTheme.typography.bodyMedium,
            color = state.status.toColor(),
        )
    }
}

@Composable
private fun BatteryRow(advertisement: AirPodsAdvertisement?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BatteryCard(
            modifier = Modifier.weight(1f),
            label = "왼쪽",
            percent = advertisement?.leftBatteryPercent,
            charging = advertisement?.leftCharging == true,
            inEar = advertisement?.leftInEar == true,
        )
        BatteryCard(
            modifier = Modifier.weight(1f),
            label = "오른쪽",
            percent = advertisement?.rightBatteryPercent,
            charging = advertisement?.rightCharging == true,
            inEar = advertisement?.rightInEar == true,
        )
        BatteryCard(
            modifier = Modifier.weight(1f),
            label = "케이스",
            percent = advertisement?.caseBatteryPercent,
            charging = advertisement?.caseCharging == true,
            inEar = false,
        )
    }
}

@Composable
private fun BatteryCard(
    modifier: Modifier = Modifier,
    label: String,
    percent: Int?,
    charging: Boolean,
    inEar: Boolean,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatPercent(percent),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = when {
                    charging -> "🔌 충전 중"
                    inEar -> "👂 착용"
                    percent == null -> ""
                    else -> "🔋"
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionRow(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(text = "›", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun TipCard(tip: Tip) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "💡 오늘의 팁",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = tip.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun DisclaimerSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.disclaimer_apple_full),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.disclaimer_samsung_full),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatPercent(percent: Int?): String =
    percent?.takeIf { it >= 0 }?.let { "$it%" } ?: "—"

@Composable
private fun PodsRepository.ConnectionStatus.toUserFacing(): String = when (this) {
    PodsRepository.ConnectionStatus.SEARCHING -> "● 검색 중"
    PodsRepository.ConnectionStatus.CONNECTED -> "● 연결됨"
    PodsRepository.ConnectionStatus.DISCONNECTED -> "○ 끊김"
    PodsRepository.ConnectionStatus.BLUETOOTH_OFF -> "△ Bluetooth 꺼짐"
    PodsRepository.ConnectionStatus.PERMISSION_DENIED -> "△ 권한 필요"
}

@Composable
private fun PodsRepository.ConnectionStatus.toColor(): Color = when (this) {
    PodsRepository.ConnectionStatus.CONNECTED -> Color(0xFF2E7D32)
    PodsRepository.ConnectionStatus.SEARCHING -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
