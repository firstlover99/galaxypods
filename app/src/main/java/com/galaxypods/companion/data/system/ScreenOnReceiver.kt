// 화면 ON 이벤트 → BLE 스캔 재시작 + Classic 모니터 재계산 (One UI 절전 복구 안전망)
package com.galaxypods.companion.data.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * `Intent.ACTION_SCREEN_ON` 수신 시 BLE 스캐너와 Bluetooth Classic 모니터를 깨운다.
 *
 * **왜.** XDA / OpenPods 커뮤니티 관찰. Samsung One UI 등 OEM 변형이 FGS BLE 스캔을
 * 깊은 절전 모드에서 throttle 또는 일시 중단. 화면 ON 시 사용자 활성 상태로
 * 인식되어 정상 우선순위 회복. 이 시점 강제로 스캐너 재시작 + Classic 상태 재질의
 * 하면 광고 캐치 확률 ↑ + UI 즉시 정확 표시.
 *
 * **수명.** FGS `onCreate`에서 [register]. `onDestroy`에서 [unregister].
 * 매니페스트 등록 X (Android 8+ implicit broadcast 제한).
 *
 * **부담.** 호출 시 idempotent 동작 (이미 동작 중이면 nothing). 화면 ON 빈도가 잦아도
 * 추가 부하 거의 0.
 */
class ScreenOnReceiver(
    private val onScreenOn: () -> Unit,
) : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == Intent.ACTION_SCREEN_ON) {
            Log.i(TAG, "SCREEN_ON received → wake scanner + recompute classic")
            onScreenOn()
        }
    }

    companion object {
        private const val TAG = "GalaxyPods/ScreenOn"

        /**
         * 등록. ACTION_SCREEN_ON은 protected broadcast이므로 manifest 등록 안 됨 — 동적 등록만.
         */
        fun register(
            context: Context,
            receiver: ScreenOnReceiver,
        ) {
            val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
            // 시스템 브로드캐스트 → EXPORTED 필요 (Android 14+).
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED,
            )
            Log.i(TAG, "registered")
        }

        fun unregister(
            context: Context,
            receiver: ScreenOnReceiver,
        ) {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
}
