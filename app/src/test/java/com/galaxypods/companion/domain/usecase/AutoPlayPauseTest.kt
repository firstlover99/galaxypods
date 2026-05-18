// AutoPlayPause UseCase — 모드별 동작 + 안전장치 검증
package com.galaxypods.companion.domain.usecase

import com.galaxypods.companion.data.system.MediaController
import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import com.galaxypods.companion.domain.model.AirPodsModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AutoPlayPauseTest {
    private lateinit var mediaController: MediaController
    private lateinit var useCase: AutoPlayPause

    @BeforeEach
    fun setUp() {
        mediaController = mockk(relaxed = true)
        every { mediaController.isMusicActive } returns true
        useCase = AutoPlayPause(mediaController)
    }

    @Test
    @DisplayName("첫 광고는 기준선만 잡고 액션 X")
    fun firstAdvertisement_noAction() {
        useCase.onAdvertisement(adWith(left = true, right = true))
        verify(exactly = 0) { mediaController.pause() }
        verify(exactly = 0) { mediaController.play() }
    }

    @Test
    @DisplayName("같은 상태 반복은 액션 X (idempotent)")
    fun sameInEarState_noAction() {
        val ad = adWith(left = true, right = true)
        useCase.onAdvertisement(ad)
        useCase.onAdvertisement(ad)
        useCase.onAdvertisement(ad)
        verify(exactly = 0) { mediaController.pause() }
    }

    @Test
    @DisplayName("enabled=false면 액션 X (그러나 상태는 추적)")
    fun disabled_noAction() {
        useCase.enabled = false
        useCase.onAdvertisement(adWith(left = true, right = true))
        useCase.onAdvertisement(adWith(left = false, right = false))
        verify(exactly = 0) { mediaController.pause() }
    }

    @Nested
    @DisplayName("RELAXED_EITHER 모드 (기본)")
    inner class RelaxedEither {
        @Test
        @DisplayName("양쪽 착용 → 한쪽만 빼면 PAUSE")
        fun bothIn_then_oneOut_triggersPause() {
            useCase.onAdvertisement(adWith(left = true, right = true))
            useCase.onAdvertisement(adWith(left = true, right = false))
            verify(exactly = 1) { mediaController.pause() }
        }

        @Test
        @DisplayName("양쪽 빠짐 → 한쪽만 끼면 재생 X (양쪽 모두 끼어야 재생)")
        fun bothOut_then_oneIn_doesNotPlay() {
            useCase.onAdvertisement(adWith(left = true, right = true))
            useCase.onAdvertisement(adWith(left = false, right = false))
            useCase.onAdvertisement(adWith(left = true, right = false))
            verify(exactly = 0) { mediaController.play() }
        }

        @Test
        @DisplayName("양쪽 빠짐 → 양쪽 다시 착용 → PLAY (단, 자동 정지 후에만)")
        fun autoPaused_then_bothIn_triggersPlay() {
            useCase.onAdvertisement(adWith(left = true, right = true))
            useCase.onAdvertisement(adWith(left = false, right = false))
            useCase.onAdvertisement(adWith(left = true, right = true))
            verify(exactly = 1) { mediaController.play() }
        }

        @Test
        @DisplayName("음악이 활성이 아니면 정지 시도 X")
        fun musicNotActive_doesNotPause() {
            every { mediaController.isMusicActive } returns false
            useCase.onAdvertisement(adWith(left = true, right = true))
            useCase.onAdvertisement(adWith(left = false, right = false))
            verify(exactly = 0) { mediaController.pause() }
        }
    }

    @Nested
    @DisplayName("STRICT_BOTH 모드")
    inner class StrictBoth {
        @BeforeEach
        fun setMode() {
            useCase.mode = AutoPlayPause.Mode.STRICT_BOTH
        }

        @Test
        @DisplayName("한쪽만 빼면 정지 X")
        fun oneOut_doesNotPause() {
            useCase.onAdvertisement(adWith(left = true, right = true))
            useCase.onAdvertisement(adWith(left = true, right = false))
            verify(exactly = 0) { mediaController.pause() }
        }

        @Test
        @DisplayName("양쪽 모두 빼야 정지")
        fun bothOut_pauses() {
            useCase.onAdvertisement(adWith(left = true, right = true))
            useCase.onAdvertisement(adWith(left = false, right = false))
            verify(exactly = 1) { mediaController.pause() }
        }

        @Test
        @DisplayName("자동 정지 후 양쪽 다시 끼면 재생")
        fun autoPaused_bothIn_plays() {
            useCase.onAdvertisement(adWith(left = true, right = true))
            useCase.onAdvertisement(adWith(left = false, right = false))
            useCase.onAdvertisement(adWith(left = true, right = true))
            verify(exactly = 1) { mediaController.play() }
        }
    }

    @Test
    @DisplayName("reset 후 다음 광고는 기준선으로 처리됨")
    fun reset_clearsState() {
        useCase.onAdvertisement(adWith(left = true, right = true))
        useCase.reset()
        // reset 후 첫 광고는 기준선 → 액션 X
        useCase.onAdvertisement(adWith(left = false, right = false))
        verify(exactly = 0) { mediaController.pause() }
    }

    @Test
    @DisplayName("우리가 정지시키지 않았으면 양쪽 착용해도 재생 안 함")
    fun notAutoPaused_bothIn_doesNotPlay() {
        // 첫 광고부터 양쪽 빠진 상태
        useCase.onAdvertisement(adWith(left = false, right = false))
        useCase.onAdvertisement(adWith(left = true, right = true))
        verify(exactly = 0) { mediaController.play() }
    }

    @Nested
    @DisplayName("Bluetooth Classic (A2DP) 끊김 기반 자동 정지/재생 — 신펌웨어 보장 경로")
    inner class ClassicDisconnect {
        @Test
        @DisplayName("Classic 끊김 → 음악 활성이면 PAUSE")
        fun classicDisconnected_pausesWhenMusicActive() {
            useCase.onClassicDisconnected()
            verify(exactly = 1) { mediaController.pause() }
        }

        @Test
        @DisplayName("Classic 끊김 → 음악 비활성이면 PAUSE X")
        fun classicDisconnected_noActionWhenMusicInactive() {
            every { mediaController.isMusicActive } returns false
            useCase.onClassicDisconnected()
            verify(exactly = 0) { mediaController.pause() }
        }

        @Test
        @DisplayName("Classic 재연결 → 우리가 정지시킨 경우에만 PLAY")
        fun classicReconnected_playsOnlyIfAutoPaused() {
            // 우리가 정지시킴
            useCase.onClassicDisconnected()
            verify(exactly = 1) { mediaController.pause() }

            // 재연결 → 재생 재개
            useCase.onClassicReconnected()
            verify(exactly = 1) { mediaController.play() }
        }

        @Test
        @DisplayName("Classic 재연결 — 정지시킨 적 없으면 PLAY X")
        fun classicReconnected_noPlayIfNotAutoPaused() {
            useCase.onClassicReconnected()
            verify(exactly = 0) { mediaController.play() }
        }

        @Test
        @DisplayName("enabled=false면 Classic 이벤트도 액션 X")
        fun classic_disabled_noAction() {
            useCase.enabled = false
            useCase.onClassicDisconnected()
            useCase.onClassicReconnected()
            verify(exactly = 0) { mediaController.pause() }
            verify(exactly = 0) { mediaController.play() }
        }
    }

    private fun adWith(
        left: Boolean,
        right: Boolean,
    ): AirPodsAdvertisement =
        AirPodsAdvertisement(
            model = AirPodsModel.AIRPODS_PRO_2_USBC,
            leftBatteryPercent = 80,
            rightBatteryPercent = 80,
            caseBatteryPercent = 80,
            leftInEar = left,
            rightInEar = right,
            leftCharging = false,
            rightCharging = false,
            caseCharging = false,
            lidOpenCount = 0,
        )
}
