// PodsRepository 구현 — BLE 광고 + Bluetooth Classic A2DP/HSP 상태 통합
package com.galaxypods.companion.data

import android.bluetooth.le.ScanResult
import android.util.Log
import com.galaxypods.companion.data.ble.AppleContinuityParser
import com.galaxypods.companion.data.ble.BleScanner
import com.galaxypods.companion.data.ble.ParserConfig
import com.galaxypods.companion.data.system.BluetoothClassicMonitor
import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.domain.repository.PodsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `PodsRepository`의 기본 구현.
 *
 * **두 신호 출처를 통합.**
 * 1. **BLE 광고 (배터리/모델 데이터)** — Type 0x07 Proximity Pairing은 페어링 핸드셰이크
 *    순간에만 송출. Type 0x10 Nearby Info는 페어링 활성 상태에서 송출되나 배터리 정보 없음.
 * 2. **Bluetooth Classic A2DP/HSP (연결 상태)** — 시스템 Bluetooth 스택이 직접 관리하는
 *    실제 오디오 연결. 사용자 화면의 "연결됨/끊김" 판단의 진실 출처.
 *
 * **상태 계산 로직.**
 * - Classic 연결됨 → CONNECTED (광고 미수신 상태에서도, 마지막 스냅샷 표시 유지)
 * - Classic 끊김 + 광고 미수신 → DISCONNECTED
 * - 어댑터 OFF / 권한 거부는 별도 상태
 *
 * **광고 갱신 정책.**
 * - Type 0x07 (model != UNKNOWN) → advertisement 갱신
 * - Type 0x10 (model == UNKNOWN) → advertisement 미갱신 (거짓 양성 방지)
 */
@Singleton
class PodsRepositoryImpl
    @Inject
    constructor(
        private val scanner: BleScanner,
        private val parser: AppleContinuityParser,
        private val classicMonitor: BluetoothClassicMonitor,
    ) : PodsRepository {
        private val _advertisement = MutableStateFlow<AirPodsAdvertisement?>(null)
        override val advertisement: StateFlow<AirPodsAdvertisement?> = _advertisement.asStateFlow()

        private val _status = MutableStateFlow(PodsRepository.ConnectionStatus.DISCONNECTED)
        override val connectionStatus: StateFlow<PodsRepository.ConnectionStatus> = _status.asStateFlow()

        private var scanning: Boolean = false
        private val supervisor: Job = SupervisorJob()
        private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + supervisor)
        private var classicObserverStarted: Boolean = false

        override fun startScanning() {
            Log.i(TAG, "startScanning called. alreadyScanning=$scanning, scannerReady=${scanner.isReady}")
            startClassicObserverIfNeeded()
            if (scanning) return
            if (!scanner.isReady) {
                Log.w(TAG, "Scanner not ready → BLUETOOTH_OFF")
                _status.value = PodsRepository.ConnectionStatus.BLUETOOTH_OFF
                return
            }
            // 초기 상태는 Classic 모니터 결과를 우선시. 끊김이면 DISCONNECTED, 연결되어 있으면 CONNECTED.
            recomputeStatusFromClassic()
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

        /** Classic 모니터 시작 + 상태 변화 구독. 한 번만 실행. */
        private fun startClassicObserverIfNeeded() {
            if (classicObserverStarted) return
            classicMonitor.start()
            classicMonitor.isAirPodsConnected
                .onEach { connected ->
                    Log.i(TAG, "Classic connected=$connected")
                    recomputeStatusFromClassic()
                    // 끊김 → advertisement clear (UI는 폴백으로 마지막 스냅샷 표시)
                    if (!connected) {
                        _advertisement.value = null
                    }
                }
                .launchIn(scope)
            classicObserverStarted = true
        }

        private fun recomputeStatusFromClassic() {
            val classicConnected = classicMonitor.isAirPodsConnected.value
            _status.value =
                if (classicConnected) {
                    PodsRepository.ConnectionStatus.CONNECTED
                } else {
                    // Classic이 아직 시작 안 됐을 수도 있으니 SEARCHING으로 부드럽게.
                    if (_status.value == PodsRepository.ConnectionStatus.CONNECTED) {
                        PodsRepository.ConnectionStatus.DISCONNECTED
                    } else {
                        PodsRepository.ConnectionStatus.SEARCHING
                    }
                }
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

            val l = parsed.leftBatteryPercent
            val r = parsed.rightBatteryPercent
            val c = parsed.caseBatteryPercent
            Log.i(TAG, "Parsed: ${parsed.model.displayName} L=$l% R=$r% case=$c%")

            // model UNKNOWN = Type 0x10 (Nearby Info) fallback → 배터리/모델 정보 없음.
            // 진짜 데이터는 Type 0x07 (Proximity Pairing) 받았을 때만.
            // 단 연결 상태는 Classic 모니터가 결정 — 광고 단독으로 CONNECTED 변경 X.
            if (parsed.model != com.galaxypods.companion.domain.model.AirPodsModel.UNKNOWN) {
                _advertisement.value = parsed
            } else {
                // Type 0x10 fallback: 광고는 받지만 정보 없음 → 화면에 표시 X
            }
        }

        companion object {
            const val DISCONNECT_TIMEOUT_MS: Long = 30_000L
            private const val TAG = "GalaxyPods/Repo"
        }
    }
