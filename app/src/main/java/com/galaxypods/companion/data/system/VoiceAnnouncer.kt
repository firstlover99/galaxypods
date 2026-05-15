// 한국어 TTS 음성 안내 — 배터리 임계값 도달/케이스 오픈 시 음성 알림
package com.galaxypods.companion.data.system

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.galaxypods.companion.domain.model.AirPodsAdvertisement
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 안드로이드 시스템 TTS를 이용해 배터리 임계값/케이스 오픈을 음성으로 알린다.
 *
 * **차용 출처.** PodsLink "Voice Broadcasting" 기능 (competitive-analysis §4.S2).
 * **사용 시나리오.** 운전 중·시각 접근성·화면 보지 않는 상황.
 *
 * **기본 설정.** 비활성. 사용자 옵트인 후 임계값(10/20/30%) 선택.
 *
 * 본 클래스는 단일 인스턴스(`@Singleton`)로 유지하며, 앱 종료 시 [shutdown]을 호출해야
 * TTS 엔진 리소스가 해제된다.
 */
@Singleton
class VoiceAnnouncer
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private var tts: TextToSpeech? = null
        private var initialized: Boolean = false
        private var pendingUtterance: String? = null

        /** 사용자 설정. 외부 [com.galaxypods.companion.data.preferences.AppPreferences]에서 주입 예정. */
        var enabled: Boolean = false

        /** 배터리 임계값(%). 0이면 임계값 알림 비활성. */
        var thresholdPercent: Int = DEFAULT_THRESHOLD_PERCENT

        /** 안내 언어. 기본 한국어. */
        var locale: Locale = Locale.KOREAN

        /**
         * TTS 엔진 초기화. 한 번만 호출. 비동기로 완료되며, 완료 전 호출된 안내는 큐잉.
         */
        fun initialize() {
            if (tts != null) return
            tts =
                TextToSpeech(context.applicationContext) { status ->
                    initialized = status == TextToSpeech.SUCCESS
                    if (initialized) {
                        tts?.language = locale
                        tts?.setOnUtteranceProgressListener(noopProgressListener)
                        pendingUtterance?.let { speak(it) }
                        pendingUtterance = null
                    }
                }
        }

        /**
         * 광고 변화에서 임계값 도달 또는 케이스 오픈 이벤트를 감지해 음성 안내.
         *
         * @param previous 직전 광고 (없으면 null)
         * @param current  최신 광고
         */
        fun announceIfNeeded(
            previous: AirPodsAdvertisement?,
            current: AirPodsAdvertisement,
        ) {
            if (!enabled) return

            val message = buildAnnouncement(previous, current) ?: return
            speak(message)
        }

        /**
         * 메시지 결정 로직. 우선순위.
         * 1) 케이스가 막 열렸음 (lidOpenCount 증가)
         * 2) 좌/우 이어버드가 임계값 이하로 떨어짐
         */
        private fun buildAnnouncement(
            previous: AirPodsAdvertisement?,
            current: AirPodsAdvertisement,
        ): String? {
            if (previous != null && current.lidOpenCount > previous.lidOpenCount) {
                return formatCaseOpen(current)
            }
            val thresholdMessage = thresholdCrossed(previous, current)
            if (thresholdMessage != null) return thresholdMessage
            return null
        }

        private fun thresholdCrossed(
            previous: AirPodsAdvertisement?,
            current: AirPodsAdvertisement,
        ): String? {
            val limit = thresholdPercent
            if (limit <= 0) return null

            val prevLeft = previous?.leftBatteryPercent ?: Int.MAX_VALUE
            val prevRight = previous?.rightBatteryPercent ?: Int.MAX_VALUE

            val leftCrossed = current.leftBatteryPercent in 0..limit && prevLeft > limit
            val rightCrossed = current.rightBatteryPercent in 0..limit && prevRight > limit

            return when {
                leftCrossed && rightCrossed -> formatBoth(current)
                leftCrossed -> formatLeft(current)
                rightCrossed -> formatRight(current)
                else -> null
            }
        }

        private fun formatCaseOpen(ad: AirPodsAdvertisement): String =
            "케이스를 열었습니다. 왼쪽 ${ad.leftBatteryPercent.coerceAtLeast(0)}퍼센트, " +
                "오른쪽 ${ad.rightBatteryPercent.coerceAtLeast(0)}퍼센트, " +
                "케이스 ${ad.caseBatteryPercent.coerceAtLeast(0)}퍼센트 남았습니다."

        private fun formatLeft(ad: AirPodsAdvertisement): String = "왼쪽 이어폰 ${ad.leftBatteryPercent}퍼센트 남았습니다."

        private fun formatRight(ad: AirPodsAdvertisement): String = "오른쪽 이어폰 ${ad.rightBatteryPercent}퍼센트 남았습니다."

        private fun formatBoth(ad: AirPodsAdvertisement): String =
            "양쪽 이어폰 모두 ${minOf(ad.leftBatteryPercent, ad.rightBatteryPercent)}퍼센트 이하입니다."

        private fun speak(message: String) {
            val engine = tts
            if (engine == null || !initialized) {
                pendingUtterance = message
                return
            }
            engine.speak(
                message,
                TextToSpeech.QUEUE_ADD,
                null,
                UUID.randomUUID().toString(),
            )
        }

        /** TTS 엔진 리소스 해제. 앱 종료 시 호출. */
        fun shutdown() {
            tts?.stop()
            tts?.shutdown()
            tts = null
            initialized = false
        }

        private val noopProgressListener: UtteranceProgressListener =
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) = Unit

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = Unit
            }

        companion object {
            const val DEFAULT_THRESHOLD_PERCENT: Int = 20
        }
    }
