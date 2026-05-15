// PodsRepositoryImpl 동작 검증 — BleScanner mock + 합성 광고
package com.galaxypods.companion.data

import com.galaxypods.companion.data.ble.AppleContinuityParser
import com.galaxypods.companion.data.ble.BleScanner
import com.galaxypods.companion.data.ble.ParserConfig
import com.galaxypods.companion.domain.model.AirPodsModel
import com.galaxypods.companion.domain.repository.PodsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Repository의 상태 전이 검증.
 *
 * 실제 BLE 콜백은 안드로이드 시스템 의존이라 mock으로 대체.
 * 본 테스트는 startScanning → 상태 전이, 권한 거부/어댑터 OFF 분기를 확인.
 */
class PodsRepositoryImplTest {
    @Test
    @DisplayName("BleScanner ready=false 시 BLUETOOTH_OFF 상태")
    fun startScanning_whenScannerNotReady_setsBluetoothOff() {
        val scanner: BleScanner = mockk(relaxed = true)
        every { scanner.isReady } returns false
        val parser = AppleContinuityParser(ParserConfig.DEFAULT) { AirPodsModel.UNKNOWN }
        val repo = PodsRepositoryImpl(scanner, parser)

        repo.startScanning()

        assertThat(repo.connectionStatus.value).isEqualTo(PodsRepository.ConnectionStatus.BLUETOOTH_OFF)
        verify(exactly = 0) { scanner.startActiveScan(any()) }
    }

    @Test
    @DisplayName("BleScanner ready=true 이면 SEARCHING 상태로 전이")
    fun startScanning_whenScannerReady_setsSearching() {
        val scanner: BleScanner = mockk(relaxed = true)
        every { scanner.isReady } returns true
        every { scanner.startActiveScan(any()) } returns true
        val parser = AppleContinuityParser(ParserConfig.DEFAULT) { AirPodsModel.UNKNOWN }
        val repo = PodsRepositoryImpl(scanner, parser)

        repo.startScanning()

        assertThat(repo.connectionStatus.value).isEqualTo(PodsRepository.ConnectionStatus.SEARCHING)
    }

    @Test
    @DisplayName("startActiveScan 실패(권한 등) 시 PERMISSION_DENIED")
    fun startScanning_whenStartFails_setsPermissionDenied() {
        val scanner: BleScanner = mockk(relaxed = true)
        every { scanner.isReady } returns true
        every { scanner.startActiveScan(any()) } returns false
        val parser = AppleContinuityParser(ParserConfig.DEFAULT) { AirPodsModel.UNKNOWN }
        val repo = PodsRepositoryImpl(scanner, parser)

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
        val repo = PodsRepositoryImpl(scanner, parser)

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
        val repo = PodsRepositoryImpl(scanner, parser)

        // 시작 안 한 상태에서 stop
        repo.stopScanning()
        verify(exactly = 0) { scanner.stopScan() }

        repo.startScanning()
        repo.stopScanning()
        verify(exactly = 1) { scanner.stopScan() }
    }
}
