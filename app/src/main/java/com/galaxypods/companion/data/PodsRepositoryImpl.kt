// PodsRepository 구현 — BleScanner의 광고를 Parser로 변환해 StateFlow로 노출
package com.galaxypods.companion.data

import android.bluetooth.le.ScanResult
import android.util.Log
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
class PodsRepositoryImpl
    @Inject
    constructor(
        private val scanner: BleScanner,
        private val parser: AppleContinuityParser,
    ) : PodsRepository {
        private val _advertisement = MutableStateFlow<AirPodsAdvertisement?>(null)
        override val advertisement: StateFlow<AirPodsAdvertisement?> = _advertisement.asStateFlow()

        private val _status = MutableStateFlow(PodsRepository.ConnectionStatus.DISCONNECTED)
        override val connectionStatus: StateFlow<PodsRepository.ConnectionStatus> = _status.asStateFlow()

        private var scanning: Boolean = false

        override fun startScanning() {
            Log.i(TAG, "startScanning called. alreadyScanning=$scanning, scannerReady=${scanner.isReady}")
            if (scanning) return
            if (!scanner.isReady) {
                Log.w(TAG, "Scanner not ready → BLUETOOTH_OFF")
                _status.value = PodsRepository.ConnectionStatus.BLUETOOTH_OFF
                return
            }
            _status.value = PodsRepository.ConnectionStatus.SEARCHING
            val started = scanner.startActiveScan(::handleResult)
            if (!started) {
                Log.w(TAG, "startActiveScan returned false → PERMISSION_DENIED")
                _status.value = PodsRepository.ConnectionStatus.PERMISSION_DENIED
                return
            }
            scanning = true
            Log.i(TAG, "Scanning started successfully")
        }

        override fun stopScanning() {
            if (!scanning) return
            scanner.stopScan()
            scanning = false
        }

        private fun handleResult(result: ScanResult) {
            val data = result.scanRecord?.getManufacturerSpecificData(ParserConfig.APPLE_MANUFACTURER_ID)
            if (data == null) {
                Log.v(TAG, "no Apple manufacturer data, skip")
                return
            }
            val hex = data.joinToString("") { "%02X".format(it) }
            Log.d(TAG, "Apple advertisement received (${data.size} bytes): $hex")

            val parsed =
                parser.parse(
                    data = data,
                    rssi = result.rssi,
                    timestamp = System.currentTimeMillis(),
                )
            if (parsed == null) {
                Log.v(TAG, "Parser returned null (not Proximity Pairing or too short)")
                return
            }

            Log.i(TAG, "Parsed: ${parsed.model.displayName} L=${parsed.leftBatteryPercent}% R=${parsed.rightBatteryPercent}% case=${parsed.caseBatteryPercent}%")

            // model UNKNOWN = Type 0x10 (Nearby Info) fallback → 배터리/모델 정보 없음.
            // 진짜 CONNECTED는 Type 0x07 (Proximity Pairing) 받았을 때만.
            // Type 0x10만 받으면 advertisement는 갱신하지 않음 (배터리 카드 비어있게 유지).
            if (parsed.model != com.galaxypods.companion.domain.model.AirPodsModel.UNKNOWN) {
                _advertisement.value = parsed
                _status.value = PodsRepository.ConnectionStatus.CONNECTED
            } else {
                // Type 0x10 fallback: 광고는 받지만 정보 없음 → 화면에 표시 X
                // (사용자가 "연결됨" 표시로 오해하는 것 방지)
            }
        }

        companion object {
            const val DISCONNECT_TIMEOUT_MS: Long = 30_000L
            private const val TAG = "GalaxyPods/Repo"
        }
    }
