// 안드로이드 시스템 미디어 키 송신 — 재생/정지/다음/이전
package com.galaxypods.companion.data.system

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 시스템 미디어 키 이벤트(KEYCODE_MEDIA_*)를 송신해 활성 미디어 세션을
 * 가진 다른 앱(Spotify, YouTube Music 등)을 제어한다.
 *
 * **차용 출처.** PodsLink 제스처 / AndroPods Voice Assistant 호출 패턴.
 *
 * **사용처.**
 * 1. 귀감지 자동 정지/재생 (`AutoPlayPause` UseCase)
 * 2. FGS Notification의 미디어 액션 버튼 (재생/정지/다음/이전)
 * 3. Voice Assistant 호출 (`KEYCODE_VOICE_ASSIST` 또는 `KEYCODE_HEADSETHOOK`)
 *
 * **주의.** 본 클래스는 미디어 세션을 직접 가지지 않는다 (Pasive controller).
 * 미디어 세션을 직접 발행해야 할 때는 별도 `MediaSessionPublisher`로 분리.
 */
@Singleton
class MediaController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val audioManager: AudioManager
        get() = context.getSystemService(AudioManager::class.java)

    /** 음악이 활성 재생 중인지 여부. 귀감지 분기에 사용. */
    val isMusicActive: Boolean
        get() = audioManager.isMusicActive

    fun play() = dispatch(KeyEvent.KEYCODE_MEDIA_PLAY)
    fun pause() = dispatch(KeyEvent.KEYCODE_MEDIA_PAUSE)
    fun playPause() = dispatch(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    fun next() = dispatch(KeyEvent.KEYCODE_MEDIA_NEXT)
    fun previous() = dispatch(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    /** Google Assistant / Bixby 호출. 활성 미디어 세션 의존하지 않음. */
    fun invokeVoiceAssistant() = dispatch(KeyEvent.KEYCODE_VOICE_ASSIST)

    private fun dispatch(keyCode: Int) {
        val time = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0),
        )
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(time, time, KeyEvent.ACTION_UP, keyCode, 0),
        )
    }
}
