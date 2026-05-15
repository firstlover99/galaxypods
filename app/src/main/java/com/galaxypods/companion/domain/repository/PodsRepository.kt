// AirPods/Beats 상태 스트림 인터페이스 — UI/UseCase가 의존하는 도메인 추상화
package com.galaxypods.companion.domain.repository

import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import kotlinx.coroutines.flow.StateFlow

/**
 * 무선 이어폰 상태(배터리, 연결, 착용)를 단일 스트림으로 제공하는 도메인 추상화.
 *
 * 구현체(`data/PodsRepositoryImpl`)는 [com.galaxypods.companion.data.ble.BleScanner]와
 * [com.galaxypods.companion.data.ble.AppleContinuityParser]를 조합해 광고를 파싱하고
 * StateFlow로 노출한다.
 *
 * **단일 진실 원천(Single Source of Truth).** UI(Compose) / FGS Notification / Widget /
 * VoiceAnnouncer가 모두 본 인터페이스를 통해 상태를 읽는다.
 */
interface PodsRepository {

    /** 현재 가장 최신 광고. 광고 미수신 시 null. */
    val advertisement: StateFlow<AirPodsAdvertisement?>

    /** 현재 연결 상태. */
    val connectionStatus: StateFlow<ConnectionStatus>

    /** 스캔 시작. 이미 동작 중이면 무시. */
    fun startScanning()

    /** 스캔 중지. */
    fun stopScanning()

    enum class ConnectionStatus { SEARCHING, CONNECTED, DISCONNECTED, BLUETOOTH_OFF, PERMISSION_DENIED }
}
