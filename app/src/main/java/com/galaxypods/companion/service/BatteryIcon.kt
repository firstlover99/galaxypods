// 배터리 % 동적 비트맵 아이콘 — FGS Notification.smallIcon용 (AndroPods 차용)
package com.galaxypods.companion.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat

/**
 * 배터리 % 텍스트를 합성한 알림 아이콘 비트맵 생성.
 *
 * **차용 출처.** AndroPods의 알림바 동적 % 표시 (competitive-analysis §4.A6).
 *
 * 256x256 논시각자료 흰색 텍스트. 알림바 단색 모드와 색상 모드 모두 호환.
 *
 * **사용 예.**
 * ```
 * val icon = BatteryIcon.createPercentIcon(85)
 * NotificationCompat.Builder(...).setSmallIcon(icon)
 * ```
 */
object BatteryIcon {

    private const val ICON_SIZE_PX = 256
    private const val TEXT_RATIO = 0.7f
    private const val DASH_RATIO = 0.5f

    /**
     * 0~100 범위의 배터리 %를 텍스트로 렌더링한 IconCompat.
     * 음수 값은 "—"로 표시 (정보 없음).
     */
    fun createPercentIcon(percent: Int): IconCompat {
        val display = formatPercent(percent)
        val bitmap = renderText(display)
        return IconCompat.createWithBitmap(bitmap)
    }

    internal fun formatPercent(percent: Int): String = when {
        percent < 0 -> "—"
        percent > 100 -> "100"
        else -> percent.toString()
    }

    private fun renderText(text: String): Bitmap {
        val bitmap = Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            isFakeBoldText = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val ratio = if (text.length <= 2) TEXT_RATIO else DASH_RATIO
        paint.textSize = ICON_SIZE_PX * ratio

        // baseline calculation
        val fontMetrics = paint.fontMetrics
        val centerY = ICON_SIZE_PX / 2f
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        val baseline = centerY + textHeight / 2 - fontMetrics.descent

        canvas.drawText(text, ICON_SIZE_PX / 2f, baseline, paint)
        return bitmap
    }
}
