// Bluetooth Classic (A2DP/HSP) 연결 상태 추적 — 진짜 "연결됨" 신호 출처
package com.galaxypods.companion.data.system

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bluetooth Classic A2DP/HSP 연결 상태를 실시간 추적.
 *
 * **왜 필요한가.**
 * BLE 광고(Type 0x07/0x10)는 페어링 활성 상태에서만 송출되며, 케이스 뚜껑이 닫혀
 * AirPods가 잠들면 광고가 끊긴다. 하지만 케이스 닫음 ≠ "사용자가 의도적으로 끊음"이
 * 아닐 수 있어 광고 단독으로는 진짜 연결 해제를 알 수 없다. A2DP/HSP 프로필 연결
 * 상태는 시스템 Bluetooth 스택이 직접 관리하므로 "지금 실제로 연결되어 있나"의
 * 진실 출처.
 *
 * **사용.**
 * - [start]를 FGS onCreate 또는 Repository 초기화 시 호출
 * - [stop]을 FGS onDestroy 시 호출
 * - [isAirPodsConnected] Flow를 구독해 UI/상태 갱신
 *
 * **AirPods 식별.**
 * 1. AAP 서비스 UUID `74ec2172-0bad-4d01-8f77-997b2be0722a` 보유 → 확정 (LibrePods/실측 검증)
 * 2. 폴백. 이름에 "airpods" / "pods" / "beats" / "powerbeats" 포함
 *
 * **권한.** `BLUETOOTH_CONNECT` (Android 12+) — 이미 매니페스트 등록됨.
 */
@Singleton
class BluetoothClassicMonitor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val bluetoothManager: BluetoothManager? =
            context.getSystemService(BluetoothManager::class.java)
        private val adapter: BluetoothAdapter? = bluetoothManager?.adapter

        private val _isAirPodsConnected = MutableStateFlow(false)
        val isAirPodsConnected: StateFlow<Boolean> = _isAirPodsConnected.asStateFlow()

        private val _connectedDeviceName = MutableStateFlow<String?>(null)
        val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

        private var a2dpProxy: BluetoothProfile? = null
        private var headsetProxy: BluetoothProfile? = null

        private val proxyListener =
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(
                    profile: Int,
                    proxy: BluetoothProfile,
                ) {
                    Log.i(TAG, "Profile proxy connected: $profile")
                    when (profile) {
                        BluetoothProfile.A2DP -> a2dpProxy = proxy
                        BluetoothProfile.HEADSET -> headsetProxy = proxy
                    }
                    recompute()
                }

                override fun onServiceDisconnected(profile: Int) {
                    Log.i(TAG, "Profile proxy disconnected: $profile")
                    when (profile) {
                        BluetoothProfile.A2DP -> a2dpProxy = null
                        BluetoothProfile.HEADSET -> headsetProxy = null
                    }
                    recompute()
                }
            }

        private val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context,
                    intent: Intent,
                ) {
                    Log.d(TAG, "Connection event: ${intent.action}")
                    recompute()
                }
            }

        private var started = false
        private val supervisor: Job = SupervisorJob()
        private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + supervisor)
        private var pollingJob: Job? = null

        fun start() {
            if (started) return
            val adapter = this.adapter
            if (adapter == null) {
                Log.w(TAG, "No Bluetooth adapter — Classic monitor disabled")
                return
            }
            adapter.getProfileProxy(context, proxyListener, BluetoothProfile.A2DP)
            adapter.getProfileProxy(context, proxyListener, BluetoothProfile.HEADSET)

            val filter =
                IntentFilter().apply {
                    addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                    addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                    addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                    addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                    addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                }
            // 시스템 브로드캐스트 → RECEIVER_EXPORTED 필요 (Android 14+).
            // NOT_EXPORTED는 같은 앱 내 브로드캐스트만 받음.
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED,
            )

            // 폴백 폴링 — 브로드캐스트 누락 / OEM 변형 대응. 5초마다 강제 재계산.
            pollingJob =
                scope.launch {
                    while (isActive) {
                        delay(POLL_INTERVAL_MS)
                        recompute()
                    }
                }

            started = true
            Log.i(TAG, "BluetoothClassicMonitor started (receiver EXPORTED + ${POLL_INTERVAL_MS}ms polling)")
        }

        fun stop() {
            if (!started) return
            pollingJob?.cancel()
            pollingJob = null
            runCatching { context.unregisterReceiver(receiver) }
            adapter?.let { a ->
                a2dpProxy?.let { a.closeProfileProxy(BluetoothProfile.A2DP, it) }
                headsetProxy?.let { a.closeProfileProxy(BluetoothProfile.HEADSET, it) }
            }
            a2dpProxy = null
            headsetProxy = null
            started = false
        }

        @SuppressLint("MissingPermission")
        private fun recompute() {
            val devices = mutableSetOf<BluetoothDevice>()
            runCatching { a2dpProxy?.connectedDevices?.let { devices.addAll(it) } }
            runCatching { headsetProxy?.connectedDevices?.let { devices.addAll(it) } }

            val airpods = devices.firstOrNull { isAirPods(it) }
            val nameForLog =
                runCatching {
                    airpods?.name ?: airpods?.address ?: "<none>"
                }.getOrDefault("<perm-denied>")
            Log.i(TAG, "recompute: ${devices.size} devices, airpods=$nameForLog")

            val wasConnected = _isAirPodsConnected.value
            val nowConnected = airpods != null
            _isAirPodsConnected.value = nowConnected
            _connectedDeviceName.value =
                runCatching { airpods?.name }.getOrNull()

            // 연결 전이 시점에 SDP 갱신 시도 — AirPods가 Type 0x07 응답하도록 자극.
            // (낮은 확률이지만 무비용. CAPod도 유사 패턴.)
            if (!wasConnected && nowConnected && airpods != null) {
                triggerSdpRefresh(airpods)
            }
        }

        /**
         * SDP 갱신 — Bluetooth Classic이 갓 연결된 시점에 호출.
         *
         * `fetchUuidsWithSdp()`는 비동기 SDP 질의를 발사. AirPods는 SDP 응답 후
         * Type 0x07 광고를 한 번 송출할 수 있음 (실측 미확정, 시도 가치).
         * 권한 거부/실패는 silent.
         */
        @SuppressLint("MissingPermission")
        private fun triggerSdpRefresh(device: BluetoothDevice) {
            runCatching {
                val success = device.fetchUuidsWithSdp()
                Log.i(TAG, "fetchUuidsWithSdp(${device.address}) → $success")
            }.onFailure {
                Log.w(TAG, "fetchUuidsWithSdp failed: ${it.message}")
            }
        }

        @SuppressLint("MissingPermission")
        private fun isAirPods(device: BluetoothDevice): Boolean =
            runCatching {
                // 1. AAP UUID 매칭 (확정)
                val uuids = device.uuids
                if (uuids != null && uuids.any { it.uuid == AAP_UUID }) {
                    return@runCatching true
                }
                // 2. 폴백 — 이름 매칭
                val name = device.name?.lowercase() ?: return@runCatching false
                NAME_PATTERNS.any { name.contains(it) }
            }.getOrDefault(false)

        companion object {
            private const val TAG = "GalaxyPods/BTClassic"

            /** 브로드캐스트 누락 시 폴백 폴링 주기. 5초마다 강제 재계산. */
            private const val POLL_INTERVAL_MS: Long = 5_000L

            /** AAP (Apple Accessory Protocol) 서비스 UUID — AirPods가 SDP에 광고. */
            private val AAP_UUID: UUID =
                UUID.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")

            /** UUID 미노출 시 폴백 — 이름 패턴 (소문자 비교). */
            private val NAME_PATTERNS: List<String> =
                listOf("airpods", "powerbeats", "beats", "pods")
        }
    }
