// PodsRepositoryImpl 동작 검증 — BleScanner + BluetoothClassicMonitor mock + 합성 광고
package com.galaxypods.companion.data

import com.galaxypods.companion.data.ble.AppleContinuityParser
import com.galaxypods.companion.data.ble.BleScanner
import com.galaxypods.companion.data.ble.ParserConfig
import com.galaxypods.companion.data.system.BluetoothClassicMonitor
import com.galaxypods.companion.domain.model.AirPodsModel
import com.galaxypods.companion.domain.repository.PodsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Repository의 상태 전이 검증.
 *
 * 실제 BLE 콜백 + 시스템 Bluetooth 프로필 프록시는 안드로이드 시스템 의존이라 mock으로 대체.
 * 본 테스트는 startScanning → 상태 전이, 권한 거부/어댑터 OFF 분기를 확인.
 */
class PodsRepositoryImplTest {
    private fun newClassicMock(connected: Boolean = false): BluetoothClassicMonitor {
        val mock: BluetoothClassicMonitor = mockk(relaxed = true)
        every { mock.isAirPodsConnected } returns MutableStateFlow(connected)
        every { mock.connectedDeviceName } returns MutableStateFlow(null)
        return mock
    }

    @Test
    @DisplayName("BleScanner ready=false 시 BLUETOOTH_OFF 상태")
    fun startScanning_whenScannerNotReady_setsBluetoothOff() {
        val scanner: BleScanner = mockk(relaxed = true)
        every { scanner.isReady } returns false
        val parser = AppleContinuityParser(ParserConfig.DEFAULT) { AirPodsModel.UNKNOWN }
        val repo = PodsRepositoryImpl(scanner, parser, newClassicMock())

        repo.startScanning()

        assertThat(repo.connectionStatus.value).isEqualTo(PodsRepository.ConnectionStatus.BLUETOOTH_OFF)
        verify(exactly = 0) { scanner.startActiveScan(any()) }
    }

    @Test
    @DisplayName("Classic 연결 없을 때 BleScanner ready=true 이면 SEARCHING 상태")
    fun startScanning_whenScannerReady_setsSearching() {
        val scanner: BleScanner = mockk(relaxed = true)
        every { scanner.isReady } returns true
        every { scanner.startActiveScan(any()) } returns true
        val parser = AppleContinuityParser(ParserConfig.DEFAULT) { AirPodsModel.UNKNOWN }
        val repo = PodsRepositoryImpl(scanner, parser, newClassicMock(connected = false))

        repo.startScanning()

        assertThat(repo.connectionStatus.value).isEqualTo(PodsRepository.ConnectionStatus.SEARCHING)
    }

    @Test
    @DisplayName("Classic 이미 연결됨 + Scanner ready → CONNECTED 즉시 진입")
    fun startScanning_whenClassicConnected_setsConnected() {
        val scanner: BleScanner = mockk(relaxed = true)
        every { scanner.isReady } returns true
        every { scanner.startActiveScan(any()) } returns true
        val parser = AppleContinuityParser(ParserConfig.DEFAULT) { AirPodsModel.UNKNOWN }
        val repo = PodsRepositoryImpl(scanner, parser, newClassicMock(connected = true))

        repo.startScanning()

        assertThat(repo.connectionStatus.value).isEqualTo(PodsRepository.ConnectionStatus.CONNECTED)
    }

    @Test
    @DisplayName("startActiveScan 실패(권한 등) 시 PERMISSION_DENIED")
    fun startScanning_whenStartFails_setsPermissionDenied() {
        val scanner: BleScanner = mockk(relaxed = true)
        every { scanner.isReady } returns true
        every { scanner.startActiveScan(any()) } returns false
        val parser = AppleContinuityParser(ParserConfig.DEFAULT) { AirPodsModel.UNKNOWN }
        val repo = PodsRepositoryImpl(scanner, parser, newClassicMock())

        repo.startScanning()

        assertThat(repo.connectionStatus.value).isEqualTo(PodsRepository.ConnectionStatus.PERMISSION_DENIED)
    }

    @Test
    @DisplayName("두 번 startScanning 호출해도 한 번만 시작")
    fun startScanning_idempotent() {
        val scanner: BleScanner = mockk(relaxed = true)
        every { scanner.isReady } returns true
        every { scanner.startActiveScan(any()) } returns true
        val parser = AppleContinuityParser(ParserConfig.DEFAULT) { AirPodsModel.UNKNOWN }
        val repo = PodsRepositoryImpl(scanner, parser, newClassicMock())

        repo.startScanning()
        repo.startScanning()

        verify(exactly = 1) { scanner.startActiveScan(any()) }
    }

    @Test
    @DisplayName("stopScanning은 시작된 적 있을 때만 동작")
    fun stopScanning_onlyAfterStart() {
        val scanner: BleScanner = mockk(relaxed = true)
        every { scanner.isReady } returns true
        every { scanner.startActiveScan(any()) } returns true
        val parser = AppleContinuityParser(ParserConfig.DEFAULT) { AirPodsModel.UNKNOWN }
        val repo = PodsRepositoryImpl(scanner, parser, newClassicMock())

        // 시작 안 한 상태에서 stop
        repo.stopScanning()
        verify(exactly = 0) { scanner.stopScan() }

        repo.startScanning()
        repo.stopScanning()
        verify(exactly = 1) { scanner.stopScan() }
    }
}
