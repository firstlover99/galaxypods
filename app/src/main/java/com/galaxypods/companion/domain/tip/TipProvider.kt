// 일일 팁 제공자 — 날짜 기반 결정적 인덱스로 매일 다른 팁 표시
package com.galaxypods.companion.domain.tip

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "오늘의 팁" 제공자.
 *
 * **차용 출처.** competitive-analysis §5 S급 5번. 사용자 리텐션 향상.
 *
 * 날짜 기반 결정적 인덱스 → 같은 날엔 같은 팁, 다음 날엔 다음 팁 (순환).
 * 외부 서버 없이 로컬 리소스로만 처리 → 데이터 전송 0 (CLAUDE.md 원칙 11).
 */
@Singleton
class TipProvider @Inject constructor() {

    fun tipOfTheDay(today: LocalDate = LocalDate.now(ZoneId.systemDefault())): Tip {
        val index = (today.toEpochDay().mod(TIPS.size.toLong())).toInt()
        return TIPS[index]
    }

    /** 전체 팁 목록 (테스트/설정 화면 미리보기용). */
    fun allTips(): List<Tip> = TIPS

    private companion object {
        // 모든 팁은 한국 사용자 시나리오 우선. 영어 버전은 v1.1 다국어 작업 시 별도.
        val TIPS: List<Tip> = listOf(
            Tip("01", "이어폰 한쪽만 빼면 음악이 자동으로 멈춰요. \"한쪽이라도\" 모드 기본값."),
            Tip("02", "한국어 음성 안내를 켜면 운전 중에도 배터리 잔량을 들을 수 있어요."),
            Tip("03", "삼성 단말은 절전 모드에서 백그라운드 동작이 막힐 수 있어요. 설정에서 \"절전 안 함\" 등록 권장."),
            Tip("04", "케이스를 두고 가도 마지막 위치를 기록해 두세요. 분실 방지에 도움이 돼요."),
            Tip("05", "AirPods Pro 3와 AirPods 4도 동일하게 지원합니다."),
            Tip("06", "Powerbeats Pro, Beats Studio Buds 같은 Beats 시리즈도 지원해요."),
            Tip("07", "위젯을 홈 화면에 추가하면 앱을 열지 않아도 배터리를 확인할 수 있어요."),
            Tip("08", "알림바의 미디어 버튼으로 재생/정지/다음/이전을 바로 컨트롤하세요."),
            Tip("09", "본 앱은 어떤 데이터도 외부로 보내지 않습니다. 모든 정보는 단말 안에서만 처리돼요."),
            Tip("10", "Bluetooth 권한에 \"위치 정보 사용 안 함\" 플래그가 부착되어 있어요."),
            Tip("11", "이어폰을 양쪽 다시 끼면 멈췄던 음악이 자동 재생됩니다."),
            Tip("12", "케이스 분실 알림을 켜면 5분 안에 50m 이상 멀어졌을 때 알림이 와요."),
            Tip("13", "다크 테마는 시스템 설정을 따라가요. Galaxy 다크 모드와 자연스럽게 어울립니다."),
            Tip("14", "FGS 알림이 거슬리면 알림 채널 설정에서 무음으로 변경할 수 있어요."),
            Tip("15", "본 앱은 Apple과 무관하며 \"AirPods\"는 Apple Inc.의 상표입니다."),
        )
    }
}
