// 메인 화면 Compose UI — 검토안 §6.2 + Tip 카드 + Settings 진입
package com.galaxypods.companion.presentation.main

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.galaxypods.companion.R
import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.domain.model.LidState
import com.galaxypods.companion.domain.model.WidgetSnapshot
import com.galaxypods.companion.domain.repository.PodsRepository
import com.galaxypods.companion.domain.tip.Tip
import com.galaxypods.companion.domain.tip.TipProvider
import com.galaxypods.companion.presentation.about.AboutScreen
import com.galaxypods.companion.presentation.location.LastLocationScreen
import com.galaxypods.companion.presentation.settings.SettingsScreen

private enum class Route { HOME, SETTINGS, LOCATION, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    var route by remember { mutableStateOf(Route.HOME) }

    when (route) {
        Route.HOME ->
            HomeContent(
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

    // 메인 화면 진입 시 FGS 자동 시작 (한 번만, 권한 있을 때)
    // BLE 스캔이 즉시 시작되어 AirPods 광고 수신 → 배터리 표시
    LaunchedEffect(Unit) {
        viewModel.startService()
    }

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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ConnectionHeader(state = state)
            Spacer(modifier = Modifier.height(8.dp))

            BatteryRow(
                advertisement = state.advertisement,
                fallbackSnapshot = state.lastSnapshot,
            )

            ToggleRow(
                title = "👂 자동 정지 (케이스 닫음)",
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

            // 배터리 갱신 안 됨 안내 — Type 0x07이 페어링 시점에만 송출되는 한계 설명
            if (state.lastSnapshot != null) {
                BatteryRefreshGuideCard()
            }

            TipCard(tip = tip)

            DisclaimerSection()
        }
    }
}

@Composable
private fun BatteryRefreshGuideCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
        shape = RoundedCornerShape(16.dp),
        onClick = {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "🔄 배터리가 갱신 안 되나요?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text =
                    "AirPods는 페어링 순간에만 배터리 정보를 보냅니다. 갱신하려면 " +
                        "Bluetooth 설정에서 한 번 연결 해제 후 다시 연결하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = "→ 탭하여 Bluetooth 설정 열기",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun ConnectionHeader(state: MainUiState) {
    val ad = state.advertisement
    val snapshot = state.lastSnapshot
    val isUnknown = ad != null && ad.model == com.galaxypods.companion.domain.model.AirPodsModel.UNKNOWN

    // 우선순위. 실시간 광고(known) > 실시간 광고(unknown) > DataStore 스냅샷 > 폴백 메시지
    val displayName =
        when {
            ad != null && !isUnknown -> ad.model.displayName
            ad != null && isUnknown -> "Apple 기기 감지됨"
            snapshot != null -> snapshot.model.displayName
            else -> "이어폰을 찾는 중"
        }
    val avatarText =
        when {
            ad != null && !isUnknown -> ad.model.displayName.firstOrNull()?.toString() ?: "—"
            ad != null && isUnknown -> "🎧"
            snapshot != null -> snapshot.model.displayName.firstOrNull()?.toString() ?: "—"
            else -> "—"
        }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = avatarText,
                fontSize = 36.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = state.status.toUserFacing(),
            style = MaterialTheme.typography.bodyMedium,
            color = state.status.toColor(),
        )
        if (isUnknown) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "배터리 확인. 케이스 뚜껑 열기 → 3초 안에 갱신",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // CAPod 정렬 — 실시간 광고일 때 lid state + pod position 표시
        if (ad != null && !isUnknown) {
            val statusBits = ad.toStatusBits()
            if (statusBits.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusBits,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // 실시간 광고 없고 마지막 스냅샷만 있을 때 → "X분 전" 표시
        if (ad == null && snapshot != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "마지막 확인. ${formatTimeAgo(snapshot.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** AirPodsAdvertisement → 사용자 friendly 상태 한 줄 (lid / pod 위치 / 마이크 등). */
private fun AirPodsAdvertisement.toStatusBits(): String {
    val parts = mutableListOf<String>()
    when (lidState) {
        LidState.OPEN -> parts.add("📦 뚜껑 열림")
        LidState.CLOSED -> parts.add("📦 뚜껑 닫힘")
        LidState.NOT_IN_CASE -> { /* 표시 생략 */ }
        LidState.UNKNOWN -> { /* 표시 생략 */ }
    }
    when {
        areBothPodsInCase -> parts.add("양쪽 케이스 안")
        isOnePodInCase -> parts.add("한쪽만 케이스")
        leftInEar && rightInEar -> parts.add("양쪽 착용")
        leftInEar -> parts.add("왼쪽 착용")
        rightInEar -> parts.add("오른쪽 착용")
    }
    return parts.joinToString("  ·  ")
}

@Composable
private fun BatteryRow(
    advertisement: AirPodsAdvertisement?,
    fallbackSnapshot: WidgetSnapshot?,
) {
    // 실시간 광고가 있으면 그것을 우선, 없으면 마지막 스냅샷을 stale 상태로 표시.
    // in-ear / charging은 stale일 때 의미 없어 false 고정 (오해 방지).
    val isStale = advertisement == null && fallbackSnapshot != null
    val left = advertisement?.leftBatteryPercent ?: fallbackSnapshot?.leftBatteryPercent
    val right = advertisement?.rightBatteryPercent ?: fallbackSnapshot?.rightBatteryPercent
    val case = advertisement?.caseBatteryPercent ?: fallbackSnapshot?.caseBatteryPercent

    // 마이크 활성 pod 표시 — CAPod isLeftPodPrimary 기반.
    // 실시간 광고 있을 때만. 양쪽 모두 귀에 있을 때만 유의미 (한쪽 케이스면 다른쪽이 자동 primary).
    val leftIsMic = advertisement?.let { it.isLeftPodPrimary && it.leftInEar && it.rightInEar } == true
    val rightIsMic = advertisement?.let { !it.isLeftPodPrimary && it.leftInEar && it.rightInEar } == true

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BatteryCard(
            modifier = Modifier.weight(1f),
            label = if (leftIsMic) "왼쪽 🎤" else "왼쪽",
            percent = left,
            charging = advertisement?.leftCharging == true,
            inEar = advertisement?.leftInEar == true,
            stale = isStale,
        )
        BatteryCard(
            modifier = Modifier.weight(1f),
            label = if (rightIsMic) "오른쪽 🎤" else "오른쪽",
            percent = right,
            charging = advertisement?.rightCharging == true,
            inEar = advertisement?.rightInEar == true,
            stale = isStale,
        )
        BatteryCard(
            modifier = Modifier.weight(1f),
            label = "케이스",
            percent = case,
            charging = advertisement?.caseCharging == true,
            inEar = false,
            stale = isStale,
        )
    }
}

@Composable
private fun BatteryCard(
    modifier: Modifier = Modifier,
    label: String,
    percent: Int?,
    @Suppress("UNUSED_PARAMETER") charging: Boolean,
    inEar: Boolean,
    stale: Boolean = false,
) {
    // 충전 표시는 v1.0에서 영구 비활성. iOS 18+ AirPods 펌웨어가 페어링 핸드셰이크 시
    // charging bit 1+2를 무조건 set하는 패턴 확인 (2026-05-18 실측). 정확한 충전 신호는
    // AAP/L2CAP (PSM 0x1001) 필요 = root = Play Store 불가. v2.0 영역.
    // stale = 마지막 스냅샷 폴백. 시각적으로 흐릿하게 + 자물쇠 아이콘으로 구별.
    val textColor =
        if (stale) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier =
                Modifier
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
                color = textColor,
            )
            Text(
                text =
                    when {
                        stale && percent != null -> "🕒 이전 값"
                        // charging 표시는 v1.0 비활성 (위 주석 참조)
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
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier =
                Modifier
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
private fun ActionRow(
    title: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
    ) {
        Row(
            modifier =
                Modifier
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
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier =
                Modifier
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
        modifier =
            Modifier
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

private fun formatPercent(percent: Int?): String = percent?.takeIf { it >= 0 }?.let { "$it%" } ?: "—"

/** 마지막 스냅샷 timestamp → 한국어 상대시간. PodsLink 패턴. */
internal fun formatTimeAgo(timestamp: Long): String {
    val diffMs = System.currentTimeMillis() - timestamp
    if (diffMs < 0) return "방금 전"
    val seconds = diffMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "방금 전"
        minutes < 60 -> "${minutes}분 전"
        hours < 24 -> "${hours}시간 전"
        days < 7 -> "${days}일 전"
        else -> "오래 전"
    }
}

@Composable
private fun PodsRepository.ConnectionStatus.toUserFacing(): String =
    when (this) {
        PodsRepository.ConnectionStatus.SEARCHING -> "● 검색 중"
        PodsRepository.ConnectionStatus.CONNECTED -> "● 연결됨"
        PodsRepository.ConnectionStatus.DISCONNECTED -> "○ 끊김"
        PodsRepository.ConnectionStatus.BLUETOOTH_OFF -> "△ Bluetooth 꺼짐"
        PodsRepository.ConnectionStatus.PERMISSION_DENIED -> "△ 권한 필요"
    }

@Composable
private fun PodsRepository.ConnectionStatus.toColor(): Color =
    when (this) {
        PodsRepository.ConnectionStatus.CONNECTED -> Color(0xFF2E7D32)
        PodsRepository.ConnectionStatus.SEARCHING -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
