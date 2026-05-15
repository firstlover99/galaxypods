// Compose Preview 모음 — Play Store 스크린샷 5장 촬영용 가이드
package com.galaxypods.companion.presentation.preview

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxypods.companion.presentation.theme.GalaxyPodsTheme

/**
 * Play Store 스크린샷 자동 캡처용 Compose Preview 모음.
 *
 * Android Studio Preview 패널에서 각 Preview를 별도 창으로 띄워 1080×2400 (Phone)
 * 해상도로 캡처. 또는 `recordRoborazzi` 같은 도구로 자동화 가능 (v1.1+).
 *
 * **데이터는 모두 합성** — 실기기 Bluetooth 의존 없이 화면 외관 검증.
 */

// ============================================================
// 스크린샷 1. 메인 화면 (배터리 표시 중) — AirPods Pro 3
// ============================================================
@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "01_메인_연결됨")
@Composable
private fun PreviewMainConnected() {
    GalaxyPodsTheme {
        FakeMainScreen(
            modelName = "AirPods Pro 3세대",
            status = "● 연결됨",
            left = 85,
            right = 82,
            case = 60,
            charging = false,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "02_메인_케이스충전")
@Composable
private fun PreviewMainCharging() {
    GalaxyPodsTheme {
        FakeMainScreen(
            modelName = "AirPods 4 (ANC)",
            status = "● 연결됨",
            left = 100,
            right = 100,
            case = 45,
            charging = true,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "03_메인_검색중")
@Composable
private fun PreviewMainSearching() {
    GalaxyPodsTheme {
        FakeMainScreen(
            modelName = "이어폰을 찾는 중",
            status = "● 검색 중",
            left = -1,
            right = -1,
            case = -1,
            charging = false,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "04_메인_Beats")
@Composable
private fun PreviewMainBeats() {
    GalaxyPodsTheme {
        FakeMainScreen(
            modelName = "Powerbeats Pro",
            status = "● 연결됨",
            left = 50,
            right = 55,
            case = 80,
            charging = false,
        )
    }
}

// ============================================================
// 스크린샷 5. Tip 카드 강조 (메인 하단)
// ============================================================
@Preview(showBackground = true, widthDp = 360, heightDp = 200, name = "05_Tip_카드")
@Composable
private fun PreviewTipCard() {
    GalaxyPodsTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "💡 오늘의 팁",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "이어폰 한쪽만 빼면 음악이 자동으로 멈춰요. \"한쪽이라도\" 모드 기본값.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

// ============================================================
// 합성 메인 화면 (실제 ViewModel 의존 없이 외관만)
// ============================================================
@Composable
private fun FakeMainScreen(
    modelName: String,
    status: String,
    left: Int,
    right: Int,
    case: Int,
    charging: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier =
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                modelName.firstOrNull()?.toString() ?: "—",
                fontSize = 36.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(modelName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(status, style = MaterialTheme.typography.bodyMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FakeBatteryCard(modifier = Modifier.weight(1f), label = "왼쪽", percent = left, charging = false)
            FakeBatteryCard(modifier = Modifier.weight(1f), label = "오른쪽", percent = right, charging = false)
            FakeBatteryCard(modifier = Modifier.weight(1f), label = "케이스", percent = case, charging = charging)
        }

        FakeToggleCard("👂 귀감지 자동 정지", checked = true)
        FakeToggleCard("🔊 한국어 음성 안내", checked = false)
    }
}

@Composable
private fun FakeBatteryCard(
    modifier: Modifier,
    label: String,
    percent: Int,
    charging: Boolean,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                if (percent < 0) "—" else "$percent%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(if (charging) "🔌 충전 중" else "🔋", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FakeToggleCard(
    title: String,
    checked: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = checked, onCheckedChange = {})
        }
    }
}
