// 삼성 One UI 분기 — 절전 정책 딥링크 + 절전 버킷 진단
package com.galaxypods.companion.data.system

import android.app.usage.UsageStatsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 삼성 One UI 전용 분기 격리 (CLAUDE.md 원칙 6).
 *
 * **책임.**
 * 1. 제조사·One UI 버전 감지 (reflection으로 SemPlatformVersion 시도, SDK 폴백)
 * 2. Samsung 절전 화면 딥링크 (One UI 6+) + 표준 폴백
 * 3. 앱이 SLEEPING 버킷에 진입했는지 자가진단 (검토안 §7.2)
 *
 * **호출 규칙.** 비-삼성 단말에서는 분기 진입 금지 ([isSamsungDevice] 사전 체크).
 * 본 클래스 외 다른 곳에 Samsung-specific 로직을 분산시키지 말 것.
 */
@Singleton
class SamsungQuirks
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        /** 본 단말이 삼성 단말인지. */
        val isSamsungDevice: Boolean
            get() = Build.MANUFACTURER.equals("samsung", ignoreCase = true)

        /**
         * One UI 메이저 버전 추정.
         *
         * 우선순위.
         * 1. `Build.VERSION.SEM_PLATFORM_INT` (Samsung 비공식 reflection) → `(value/10000) - 9`
         *    예. 130000 → One UI 4, 150000 → One UI 6, 160000 → One UI 7
         * 2. SDK_INT 기반 추정 폴백 (정확하지 않지만 분기에는 충분)
         *
         * 비-삼성 단말은 null 반환.
         */
        fun oneUiMajorVersion(): Int? {
            if (!isSamsungDevice) return null

            readSemPlatformVersion()?.let { return (it / 10000) - 9 }

            // 폴백. Android 버전 기반 추정 (One UI는 Samsung이 별도 업데이트하지만 대략 일치)
            return when (Build.VERSION.SDK_INT) {
                Build.VERSION_CODES.R -> 3 // Android 11 → One UI 3
                Build.VERSION_CODES.S, Build.VERSION_CODES.S_V2 -> 4 // Android 12/12L → One UI 4
                Build.VERSION_CODES.TIRAMISU -> 5 // Android 13 → One UI 5
                Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> 6 // Android 14 → One UI 6
                Build.VERSION_CODES.VANILLA_ICE_CREAM -> 7 // Android 15 → One UI 7
                else -> if (Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM) 8 else null
            }
        }

        /** Samsung 절전 화면을 직접 연다 (One UI 6+ 권장). 미지원 시 표준 폴백. */
        fun openBatteryOptimizationSettings() {
            val opened = tryOpenSamsungSleepingApps()
            if (!opened) openStandardBatteryOptimization()
        }

        /**
         * Samsung Smart Manager의 "Never Sleeping Apps" 화면 직접 진입.
         *
         * 비공식 Activity (`com.samsung.android.sm.ui.battery.BatteryActivity`,
         * `activity_type=2`). One UI 마이너 업데이트로 변경 가능 → try/catch + 폴백 필수.
         */
        private fun tryOpenSamsungSleepingApps(): Boolean {
            if (!isSamsungDevice) return false
            return runCatching {
                val intent =
                    Intent().apply {
                        setClassName(
                            "com.samsung.android.sm",
                            "com.samsung.android.sm.ui.battery.BatteryActivity",
                        )
                        putExtra("activity_type", 2)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(intent)
                true
            }.getOrElse {
                if (it is ActivityNotFoundException) false else throw it
            }
        }

        /** Android 표준 배터리 최적화 예외 화면. */
        private fun openStandardBatteryOptimization() {
            val intent =
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            runCatching { context.startActivity(intent) }.getOrElse {
                // 최종 폴백 — 단순 배터리 설정 화면
                val fallback =
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                runCatching { context.startActivity(fallback) }
            }
        }

        /** 본 앱이 배터리 최적화 예외 목록에 있는지. */
        fun isIgnoringBatteryOptimizations(): Boolean {
            val pm = context.getSystemService(PowerManager::class.java) ?: return false
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }

        /**
         * 본 앱이 SLEEPING 버킷에 진입했는지 자가진단 (검토안 §7.2).
         * RARE 이상이면 Samsung 절전 정책에 의해 백그라운드 동작이 제한될 수 있음.
         */
        fun sleepStatus(): SleepStatus {
            // appStandbyBucket은 API 28+ (Android 9+). minSdk 26~27는 UNKNOWN.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return SleepStatus.UNKNOWN
            val usm =
                context.getSystemService(UsageStatsManager::class.java)
                    ?: return SleepStatus.UNKNOWN
            val bucket = usm.appStandbyBucket
            return when {
                bucket >= UsageStatsManager.STANDBY_BUCKET_RARE -> SleepStatus.SLEEPING
                bucket >= UsageStatsManager.STANDBY_BUCKET_FREQUENT -> SleepStatus.NORMAL
                else -> SleepStatus.ACTIVE
            }
        }

        /**
         * `android.os.Build.VERSION.SEM_PLATFORM_INT`를 reflection으로 읽는다.
         * Samsung 단말 외에는 필드 없음 → null.
         */
        private fun readSemPlatformVersion(): Int? =
            runCatching {
                val versionClass = Class.forName("android.os.Build\$VERSION")
                val field = versionClass.getField("SEM_PLATFORM_INT")
                field.getInt(null)
            }.getOrNull()

        enum class SleepStatus { ACTIVE, NORMAL, SLEEPING, UNKNOWN }
    }
