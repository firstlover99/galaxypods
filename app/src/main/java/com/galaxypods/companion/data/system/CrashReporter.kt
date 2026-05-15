// Crashlytics 통합 골격 — 옵트인 기반, 빌드 활성화 후 실제 SDK 연결
package com.galaxypods.companion.data.system

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 크래시 리포팅 추상화.
 *
 * **CLAUDE.md 원칙 11.** 사용자 데이터 외부 전송 금지가 기본. Crashlytics는
 * 옵트인 시에만 활성화. 옵트아웃 시 본 인터페이스의 모든 메서드는 no-op.
 *
 * **현재 상태.** 골격만 구현. 실제 Firebase Crashlytics SDK 연결은 다음 조건 충족 후.
 * 1. Firebase 프로젝트 생성 + `google-services.json` 추가
 * 2. `app/build.gradle.kts`에서 `google-services` / `firebase-crashlytics` 플러그인 활성화
 * 3. [setCrashlyticsCollectionEnabled] 구현체에서 실제 SDK 호출
 * 4. 개인정보처리방침에 Firebase 명시 (이미 작성됨, docs/privacy-{ko,en}.md §4)
 *
 * 골격 단계에서는 logcat 출력만.
 */
@Singleton
class CrashReporter
    @Inject
    constructor() {
        private var enabled: Boolean = false

        /** 사용자 옵트인/아웃 반영. SDK 활성화 후엔 FirebaseCrashlytics 인스턴스 호출. */
        fun setEnabled(enabled: Boolean) {
            this.enabled = enabled
            Log.i(TAG, "Crashlytics collection ${if (enabled) "ENABLED" else "DISABLED"}")
            // TODO: Firebase 연결 후
            // FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
        }

        /** 비치명적 예외 보고. 옵트아웃 시 무동작. */
        fun recordException(
            throwable: Throwable,
            attributes: Map<String, String> = emptyMap(),
        ) {
            if (!enabled) return
            Log.w(TAG, "[Crashlytics] ${throwable.javaClass.simpleName}: ${throwable.message}")
            attributes.forEach { (k, v) -> Log.w(TAG, "  $k=$v") }
            // TODO:
            // val fc = FirebaseCrashlytics.getInstance()
            // attributes.forEach { (k, v) -> fc.setCustomKey(k, v) }
            // fc.recordException(throwable)
        }

        /** 사용자 식별자 설정 금지 — PII 수집 정책 위반 ([CLAUDE.md] 원칙 11). */
        @Deprecated("PII 수집 금지. 사용자 식별자 설정하지 말 것.", level = DeprecationLevel.ERROR)
        @Suppress("UnusedParameter", "unused")
        fun setUserId(userId: String): Unit = error("PII 수집 금지")

        private companion object {
            const val TAG: String = "GalaxyPods/Crash"
        }
    }
