// 케이스 오픈 감지 UseCase — lidOpenCount 변화 콜백
package com.galaxypods.companion.domain.usecase

import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 광고 패킷의 `lidOpenCount` 증가를 감지해 케이스 오픈 이벤트를 콜백으로 알린다.
 *
 * **검토안 §3.1 P1 케이스 오픈 팝업 / §6.3 케이스 오픈 팝업 레이아웃** 참조.
 *
 * **v1.0 결정 (CLAUDE.md 원칙 12).** 풀스크린 알림으로 노출. SYSTEM_ALERT_WINDOW
 * 오버레이는 v1.1로 보류.
 *
 * **부가 안전장치.**
 * - 첫 광고는 기준선만 잡고 무동작
 * - 같은 카운트 반복은 무동작 (멱등)
 * - 카운트가 감소(롤오버 또는 새 케이스)하면 새 기준선으로 재설정 후 무동작
 * - 짧은 시간 내 반복 트리거 방지 ([COOLDOWN_MS])
 */
@Singleton
class CaseOpenDetect
    @Inject
    constructor() {
        /** 사용자 설정. 향후 [com.galaxypods.companion.data.preferences.AppPreferences] 위임. */
        var enabled: Boolean = true

        private var lastLidOpenCount: Int = -1
        private var lastTriggerAtMs: Long = 0L

        /**
         * 광고 한 프레임 처리. 케이스 오픈 시 [onCaseOpened] 호출.
         *
         * @param ad 최신 광고
         * @param nowMs 현재 시각 ms (테스트 주입 가능)
         * @param onCaseOpened 케이스 오픈 시 호출. 인자는 트리거된 광고.
         */
        fun onAdvertisement(
            ad: AirPodsAdvertisement,
            nowMs: Long = System.currentTimeMillis(),
            onCaseOpened: (AirPodsAdvertisement) -> Unit,
        ) {
            if (!enabled) {
                lastLidOpenCount = ad.lidOpenCount
                return
            }

            val previous = lastLidOpenCount
            lastLidOpenCount = ad.lidOpenCount

            if (previous < 0) return // 첫 광고
            if (ad.lidOpenCount <= previous) return // 동일 또는 감소

            // 첫 트리거(lastTriggerAtMs == 0)는 쿨다운 우회. 두 번째부터 쿨다운 적용.
            if (lastTriggerAtMs > 0 && nowMs - lastTriggerAtMs < COOLDOWN_MS) return

            lastTriggerAtMs = nowMs
            onCaseOpened(ad)
        }

        fun reset() {
            lastLidOpenCount = -1
            lastTriggerAtMs = 0L
        }

        companion object {
            const val COOLDOWN_MS: Long = 2_000L
        }
    }
