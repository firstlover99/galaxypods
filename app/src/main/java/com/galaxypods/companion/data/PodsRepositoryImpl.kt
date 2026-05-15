// PodsRepository 구현 — BleScanner의 광고를 Parser로 변환해 StateFlow로 노출
package com.galaxypods.companion.data

import android.bluetooth.le.ScanResult
import com.galaxypods.companion.data.ble.AppleContinuityParser
import com.galaxypods.companion.data.ble.BleScanner
import com.galaxypods.companion.data.ble.ParserConfig
import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.domain.repository.PodsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `PodsRepository`의 기본 구현.
 *
 * 동작 흐름.
 * 1. [startScanning] → BleScanner 활성 스캔 시작
 * 2. ScanResult 콜백 → manufacturerSpecificData(0x004C) 추출
 * 3. AppleContinuityParser로 파싱 → StateFlow 갱신
 * 4. UI / FGS Notification / Widget / VoiceAnnouncer가 StateFlow 구독
 *
 * **연결 상태 휴리스틱.**
 * - 광고 수신 → CONNECTED
 * - [DISCONNECT_TIMEOUT_MS] 동안 광고 미수신 → DISCONNECTED (FGS 알림 갱신 시 재계산)
 * - 어댑터 OFF / 권한 거부는 별도 상태
 */
@Singleton
class PodsRepositoryImpl @Inject constructor(
    private val scanner: BleScanner,
    private val parser: AppleContinuityParser,
) : PodsRepository {

    private val _advertisement = MutableStateFlow<AirPodsAdvertisement?>(null)
    override val advertisement: StateFlow<AirPodsAdvertisement?> = _advertisement.asStateFlow()

    private val _status = MutableStateFlow(PodsRepository.ConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<PodsRepository.ConnectionStatus> = _status.asStateFlow()

    private var scanning: Boolean = false

    override fun startScanning() {
        if (scanning) return
        if (!scanner.isReady) {
            _status.value = PodsRepository.ConnectionStatus.BLUETOOTH_OFF
            return
        }
        _status.value = PodsRepository.ConnectionStatus.SEARCHING
        val started = scanner.startActiveScan(::handleResult)
        if (!started) {
            _status.value = PodsRepository.ConnectionStatus.PERMISSION_DENIED
            return
        }
        scanning = true
    }

    override fun stopScanning() {
        if (!scanning) return
        scanner.stopScan()
        scanning = false
    }

    private fun handleResult(result: ScanResult) {
        val data = result.scanRecord
            ?.getManufacturerSpecificData(ParserConfig.APPLE_MANUFACTURER_ID)
            ?: return

        val parsed = parser.parse(
            data = data,
            rssi = result.rssi,
            timestamp = System.currentTimeMillis(),
        ) ?: return

        _advertisement.value = parsed
        _status.value = PodsRepository.ConnectionStatus.CONNECTED
    }

    companion object {
        const val DISCONNECT_TIMEOUT_MS: Long = 30_000L
    }
}
