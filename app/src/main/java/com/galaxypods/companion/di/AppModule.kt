// Hilt DI 모듈 — Singleton 객체 제공 + Repository 인터페이스 바인딩
package com.galaxypods.companion.di

import com.galaxypods.companion.data.PodsRepositoryImpl
import com.galaxypods.companion.data.ble.AirPodsModelTable
import com.galaxypods.companion.data.ble.AppleContinuityParser
import com.galaxypods.companion.data.ble.ParserConfig
import com.galaxypods.companion.domain.repository.PodsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 앱 수명 동안 단일 인스턴스로 유지되는 객체들의 바인딩.
 *
 * **원칙.** 모든 데이터/시스템 클래스는 `@Singleton`. ViewModel은 `@HiltViewModel`로
 * presentation 레이어에서 별도 처리.
 *
 * `BleScanner`, `MediaController`, `VoiceAnnouncer`는 `@Inject constructor`로
 * Hilt가 자동 인식하므로 본 모듈에 별도 등록 불필요. ApplicationContext는
 * Hilt가 기본 제공.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideParserConfig(): ParserConfig = ParserConfig.DEFAULT

    @Provides
    @Singleton
    fun provideContinuityParser(config: ParserConfig): AppleContinuityParser =
        AppleContinuityParser(config = config, modelTable = AirPodsModelTable::identify)
}

/**
 * 인터페이스 → 구현 바인딩. 별도 모듈로 분리해 `@Binds` 패턴 명시.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPodsRepository(impl: PodsRepositoryImpl): PodsRepository
}
