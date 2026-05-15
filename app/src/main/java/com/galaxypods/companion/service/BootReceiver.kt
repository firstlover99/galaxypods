// 부팅 / 앱 업데이트 후 FGS 자동 시작 — Android 10+ FGS 시작 제한 대응
package com.galaxypods.companion.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` 수신 후 FGS를 시작한다.
 *
 * **Android 10+ FGS 시작 제한 주의 (CLAUDE.md 검토안 §4.2 검토 권장 7).**
 * BOOT_COMPLETED 컨텍스트에서 startForegroundService()는 실행되지만 일부 OEM
 * (특히 Xiaomi/Huawei)에서 즉시 종료될 수 있음. Samsung One UI는 비교적 관대하나
 * 사용자가 절전 예외 미등록 시 차단 가능 (온보딩에서 안내 권장).
 *
 * 본 리시버는 권한 사전 검사 후 FGS를 시작하고, 실패 시 silent 폴백.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> tryStartService(context)
            else -> Unit
        }
    }

    private fun tryStartService(context: Context) {
        if (!hasRequiredPermissions(context)) return
        runCatching { PodsForegroundService.start(context) }
    }

    /**
     * FGS 시작에 최소한 필요한 권한 사전 검사.
     * 권한 부족 시 silent return (사용자가 앱 첫 실행 시 권한 허용 후 자동 동작).
     */
    private fun hasRequiredPermissions(context: Context): Boolean {
        val needed =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.Manifest.permission.BLUETOOTH_SCAN
            } else {
                android.Manifest.permission.BLUETOOTH_ADMIN
            }
        return ContextCompat.checkSelfPermission(context, needed) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
