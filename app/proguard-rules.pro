# GalaxyPods Proguard 규칙 — 릴리스 빌드 R8 최소화

# Kotlin 표준
-dontwarn kotlin.**
-keepattributes *Annotation*, InnerClasses
-keep class kotlin.Metadata { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keep,allowobfuscation @interface dagger.hilt.android.lifecycle.HiltViewModel

# Data 모델 (직렬화될 가능성 → 일단 보존)
-keep class com.galaxypods.companion.domain.model.** { *; }

# AppleContinuityParser는 비트 오프셋 의존 → 난독화 허용 (내부 메서드명만)
# 단, 공개 API는 보존
-keep public class com.galaxypods.companion.data.ble.AppleContinuityParser {
    public *;
}

# DataStore
-keep class androidx.datastore.preferences.** { *; }

# Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
