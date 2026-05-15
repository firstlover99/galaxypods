// FGS Notification 미디어 액션 라우팅 — BroadcastReceiver 기반
package com.galaxypods.companion.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.galaxypods.companion.data.system.MediaController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * FGS 알림에 표시되는 미디어 컨트롤 버튼이 눌렸을 때 [MediaController]로 라우팅한다.
 *
 * **차용 출처.** PodsLink 알림 액션 미디어 컨트롤 (competitive-analysis §4.S3).
 *
 * 알림 빌더에서 본 리시버 대상 PendingIntent를 액션 버튼에 부착한다.
 *
 * 사용 예 (PodsForegroundService에서).
 * ```
 * NotificationCompat.Builder(this, CHANNEL_FGS)
 *     .addAction(R.drawable.ic_skip_previous, "이전", NotificationActions.pendingIntent(this, ACTION_PREVIOUS))
 *     .addAction(R.drawable.ic_play_arrow,    "재생", NotificationActions.pendingIntent(this, ACTION_PLAY_PAUSE))
 *     .addAction(R.drawable.ic_skip_next,     "다음", NotificationActions.pendingIntent(this, ACTION_NEXT))
 * ```
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject lateinit var mediaController: MediaController

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            ACTION_PLAY_PAUSE -> mediaController.playPause()
            ACTION_NEXT -> mediaController.next()
            ACTION_PREVIOUS -> mediaController.previous()
            ACTION_VOICE_ASSIST -> mediaController.invokeVoiceAssistant()
            else -> Unit
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.galaxypods.companion.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.galaxypods.companion.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.galaxypods.companion.ACTION_PREVIOUS"
        const val ACTION_VOICE_ASSIST = "com.galaxypods.companion.ACTION_VOICE_ASSIST"

        /** 알림 액션 버튼에 부착할 PendingIntent를 생성. */
        fun pendingIntent(
            context: Context,
            action: String,
        ): PendingIntent {
            val intent =
                Intent(context, NotificationActionReceiver::class.java).apply {
                    this.action = action
                    setPackage(context.packageName)
                }
            return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /** 본 리시버를 동적 등록. FGS onCreate에서 호출, onDestroy에서 해제. */
        fun register(
            context: Context,
            receiver: NotificationActionReceiver,
        ) {
            val filter =
                IntentFilter().apply {
                    addAction(ACTION_PLAY_PAUSE)
                    addAction(ACTION_NEXT)
                    addAction(ACTION_PREVIOUS)
                    addAction(ACTION_VOICE_ASSIST)
                }
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
    }
}
