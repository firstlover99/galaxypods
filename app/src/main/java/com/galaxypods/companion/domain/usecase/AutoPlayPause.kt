// 귀감지 자동 정지/재생 UseCase — in-ear 변화 감지 후 미디어 키 송신
package com.galaxypods.companion.domain.usecase

import com.galaxypods.companion.data.system.MediaController
import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AirPods/Beats의 in-ear 비트 변화로 미디어 재생을 자동 제어한다.
 *
 * **차용/개선 포인트.** AndroPods는 귀감지가 Pro IAP 잠금. GalaxyPods는 영구 무료
 * (CLAUDE.md 원칙 13).
 *
 * **로직 (모드별).**
 *
 * - [Mode.STRICT_BOTH] — 양쪽 모두 빼야 정지. 양쪽 모두 다시 끼면 재생.
 * - [Mode.RELAXED_EITHER] (기본) — 한쪽이라도 빼면 정지. 양쪽 모두 다시 끼면 재생.
 *
 * **자동 재생 안전장치.**
 * 1. 우리가 정지시킨 경우에만 자동 재생([wasAutoPaused] 플래그)
 * 2. 사용자가 직접 다른 곳에서 정지/재생한 흐름은 가급적 방해하지 않도록 단순화
 *
 * **사용처.** [com.galaxypods.companion.service.PodsForegroundService]가 광고 콜백마다
 * [onAdvertisement]를 호출.
 */
@Singleton
class AutoPlayPause
    @Inject
    constructor(
        private val mediaController: MediaController,
    ) {
        /** 사용자 설정. 향후 `AppPreferences`에서 주입 (Phase 4). */
        var enabled: Boolean = true

        /** 정지/재생 트리거 모드. */
        var mode: Mode = Mode.RELAXED_EITHER

        private var lastInEar: InEarState? = null
        private var wasAutoPaused: Boolean = false

        /**
         * 광고 한 프레임 처리. 본 메서드는 idempotent (같은 광고 중복 호출에도 액션 1회만).
         */
        fun onAdvertisement(ad: AirPodsAdvertisement) {
            if (!enabled) {
                // 비활성 상태에서도 inEar 추적은 유지 (재활성화 시 정합성 위해)
                lastInEar = InEarState(ad.leftInEar, ad.rightInEar)
                return
            }

            val current = InEarState(ad.leftInEar, ad.rightInEar)
            val previous = lastInEar
            lastInEar = current

            if (previous == null || previous == current) return

            when (computeAction(previous, current)) {
                Action.PAUSE -> handlePause()
                Action.PLAY -> handlePlay()
                Action.NONE -> Unit
            }
        }

        /**
         * Bluetooth Classic (A2DP/HSP) 연결 끊김 이벤트 처리.
         *
         * **시나리오.** 사용자가 AirPods 양쪽을 케이스에 넣고 뚜껑 닫음 → A2DP 끊김 →
         * 본 메서드 호출 → 활성 미디어 자동 정지.
         *
         * **왜 별도 경로인가.** 신펌웨어 AirPods Pro가 Type 0x07 (in-ear 비트 포함) 광고를
         * 사실상 송출하지 않아 BLE 기반 [onAdvertisement]가 동작하지 않음. A2DP 끊김은
         * 시스템 보장 신호 — 신펌웨어/구펌웨어 무관, 100% 동작.
         *
         * **단일 pod 제거.** A2DP 유지되므로 본 경로 트리거 X — 불가능. 이는 비루트 한계
         * 이며 모든 OSS 앱(AndroPods/PodsLink 포함) 공통.
         */
        fun onClassicDisconnected() {
            if (!enabled) return
            handlePause()
        }

        /**
         * Bluetooth Classic 재연결 이벤트.
         *
         * **시나리오.** 사용자가 케이스 다시 열어서 AirPods 귀에 착용 → A2DP 재연결 →
         * 본 메서드 호출 → 우리가 자동 정지시킨 경우에만 재생 재개.
         */
        fun onClassicReconnected() {
            if (!enabled) return
            handlePlay()
        }

        /** 외부에서 강제 리셋 (사용자가 토글 OFF 후 ON, 페어링 변경 등). */
        fun reset() {
            lastInEar = null
            wasAutoPaused = false
        }

        private fun computeAction(
            previous: InEarState,
            current: InEarState,
        ): Action {
            return when (mode) {
                Mode.STRICT_BOTH -> {
                    val wasBothIn = previous.left && previous.right
                    val isBothIn = current.left && current.right
                    val wasBothOut = !previous.left && !previous.right
                    val isBothOut = !current.left && !current.right

                    when {
                        wasBothIn && isBothOut -> Action.PAUSE
                        wasBothOut && isBothIn -> Action.PLAY
                        else -> Action.NONE
                    }
                }
                Mode.RELAXED_EITHER -> {
                    // 한쪽이라도 빼면 정지. 양쪽 모두 다시 끼면 재생.
                    val wasBothIn = previous.left && previous.right
                    val isAnyOut = !current.left || !current.right
                    val wasAnyOut = !previous.left || !previous.right
                    val isBothIn = current.left && current.right

                    when {
                        wasBothIn && isAnyOut -> Action.PAUSE
                        wasAnyOut && isBothIn -> Action.PLAY
                        else -> Action.NONE
                    }
                }
            }
        }

        private fun handlePause() {
            if (mediaController.isMusicActive) {
                mediaController.pause()
                wasAutoPaused = true
            }
        }

        private fun handlePlay() {
            if (wasAutoPaused) {
                mediaController.play()
                wasAutoPaused = false
            }
        }

        enum class Mode { STRICT_BOTH, RELAXED_EITHER }

        private enum class Action { PAUSE, PLAY, NONE }

        private data class InEarState(val left: Boolean, val right: Boolean)
    }
