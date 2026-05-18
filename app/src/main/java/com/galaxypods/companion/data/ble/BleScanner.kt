// Apple Continuity 광고 BLE 스캐너 — manufacturerData 마스크로 Type 0x07 사전 필터
package com.galaxypods.companion.data.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bluetooth Low Energy 스캐너 래퍼.
 *
 * **CLAUDE.md 원칙 5 — ScanFilter 정밀화 필수.**
 * `manufacturerId=0x004C` 단독 매칭은 iPhone/Apple Watch/Mac 광고를 모두 잡아
 * 콜백 폭주 + 배터리 소모. 본 스캐너는 **manufacturerData 마스크**로
 * `[Type=0x07, Length=0x19]` 헤더가 있는 Continuity Proximity Pairing 광고만
 * 시스템 레벨에서 통과시킨다.
 *
 * **모드.**
 * - [startActiveScan] — Foreground / FGS 활성 시. 즉시 콜백, 배터리 소모 큼.
 * - [startPassiveScan] (TODO) — 백그라운드. PendingIntent 기반, 스로틀 면제 (Phase 2.5).
 */
@Singleton
class BleScanner
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val bluetoothManager: BluetoothManager?
            get() = context.getSystemService(BluetoothManager::class.java)

        private val adapter: BluetoothAdapter?
            get() = bluetoothManager?.adapter

        private var activeCallback: ScanCallback? = null

        /** Bluetooth가 켜져 있고 스캔 권한이 부여되어 있는지. */
        val isReady: Boolean
            get() = adapter?.isEnabled == true && hasScanPermission()

        /**
         * Foreground / FGS 환경 active scan 시작.
         *
         * @param onResult 광고 수신 콜백. UI 스레드 외에서 호출됨.
         * @return 시작 성공 여부. (어댑터 OFF 또는 권한 거부 시 false)
         */
        @SuppressLint("MissingPermission")
        fun startActiveScan(onResult: (ScanResult) -> Unit): Boolean {
            Log.i(TAG, "startActiveScan called. ready=$isReady, adapter=${adapter != null}, permission=${hasScanPermission()}")
            if (!isReady) {
                Log.w(TAG, "Not ready — bluetooth off or permission denied")
                return false
            }
            val scanner = adapter?.bluetoothLeScanner
            if (scanner == null) {
                Log.w(TAG, "bluetoothLeScanner is null")
                return false
            }

            // 이미 스캔 중이면 중지 후 재시작
            stopScan()

            val callback =
                object : ScanCallback() {
                    override fun onScanResult(
                        callbackType: Int,
                        result: ScanResult,
                    ) {
                        Log.d(TAG, "onScanResult: addr=${result.device?.address} rssi=${result.rssi}")
                        onResult(result)
                    }

                    override fun onBatchScanResults(results: MutableList<ScanResult>) {
                        Log.d(TAG, "onBatchScanResults: ${results.size} items")
                        results.forEach(onResult)
                    }

                    override fun onScanFailed(errorCode: Int) {
                        Log.e(TAG, "onScanFailed errorCode=$errorCode")
                    }
                }
            activeCallback = callback

            scanner.startScan(buildFilters(), buildSettings(), callback)
            Log.i(TAG, "startScan invoked")
            return true
        }

        /** 스캔 중지. 등록된 콜백 해제. */
        @SuppressLint("MissingPermission")
        fun stopScan() {
            if (!isReady) {
                activeCallback = null
                return
            }
            val scanner = adapter?.bluetoothLeScanner ?: return
            activeCallback?.let(scanner::stopScan)
            activeCallback = null
        }

        /**
         * Apple manufacturer ID(0x004C) 광고만 시스템 레벨에서 통과.
         *
         * **변경 (2026-05-18).** Type/Length 마스크 제거 — 일부 펌웨어/모델은 다른 길이의
         * 광고를 송출. 우리 파서가 코드 레벨에서 Type 0x07 검증하므로 안전.
         * 콜백 부하는 약간 증가하나 iPhone/Watch 광고는 빠르게 거부됨.
         */
        private fun buildFilters(): List<ScanFilter> {
            val filter =
                ScanFilter.Builder()
                    .setManufacturerData(ParserConfig.APPLE_MANUFACTURER_ID, byteArrayOf(), byteArrayOf())
                    .build()
            return listOf(filter)
        }

        /** 활성 스캔 설정. 디버그 단계는 LOW_LATENCY로 빠른 캡처. */
        private fun buildSettings(): ScanSettings {
            val builder =
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                builder.setNumOfMatches(ScanSettings.MATCH_NUM_FEW_ADVERTISEMENT)
            }
            return builder.build()
        }

        private companion object {
            const val TAG = "GalaxyPods/BLE"
        }

        private fun hasScanPermission(): Boolean {
            // Android 12+. BLUETOOTH_SCAN
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN,
                ) == PackageManager.PERMISSION_GRANTED
            }
            // Android 11 이하. BLUETOOTH_ADMIN + ACCESS_FINE_LOCATION 둘 다
            val admin =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADMIN,
                ) == PackageManager.PERMISSION_GRANTED
            val location =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
            return admin && location
        }
    }
