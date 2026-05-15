// 마지막 위치 화면 — Google Maps Compose Lite 모드
package com.galaxypods.companion.presentation.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.galaxypods.companion.domain.model.LastLocation
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 마지막으로 이어폰 연결이 끊긴 위치 표시.
 *
 * - 위치 기록 있음 → 지도 + 좌표 + 기록 시각 + 삭제 버튼
 * - 위치 기록 없음 → 안내 메시지
 *
 * 지도는 Compose Maps의 일반 모드 (Lite 모드는 Compose Maps에서 직접 제공 안 됨).
 * 성능 영향 최소화 위해 인터랙션 제한 (zoom controls X, indoor X).
 */
@Composable
fun LastLocationScreen(
    onBack: () -> Unit,
    viewModel: LastLocationViewModel = hiltViewModel(),
) {
    val location by viewModel.lastLocation.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "마지막 위치",
            style = MaterialTheme.typography.headlineSmall,
        )

        if (location == null) {
            EmptyStateCard()
        } else {
            MapCard(location = location!!)
            DetailCard(location = location!!)
            ActionRow(
                onClear = viewModel::clear,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "📍 기록된 위치가 없습니다",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "이어폰과의 연결이 끊기는 시점에 한 번 위치를 기록합니다. " +
                    "설정에서 \"마지막 위치 기록\"을 켜고 위치 권한을 허용하세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MapCard(location: LastLocation) {
    val target = LatLng(location.latitude, location.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(target, MAP_ZOOM)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = false,
                isIndoorEnabled = false,
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = false,
                myLocationButtonEnabled = false,
                rotationGesturesEnabled = false,
                tiltGesturesEnabled = false,
            ),
        ) {
            Marker(
                state = MarkerState(position = target),
                title = "마지막 위치",
            )
        }
    }
}

@Composable
private fun DetailCard(location: LastLocation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = "기록 시각. ${formatTimestamp(location.timestamp)}")
            Text(
                text = "좌표. %.6f, %.6f".format(location.latitude, location.longitude),
                style = MaterialTheme.typography.bodySmall,
            )
            location.accuracyMeters?.let {
                Text(
                    text = "정확도. ±${it.toInt()}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(onClear: () -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
                Text("위치 기록 삭제")
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("돌아가기")
            }
        }
    }
}

private fun formatTimestamp(ts: Long): String {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREAN)
    return format.format(Date(ts))
}

private const val MAP_ZOOM: Float = 16f
