// 일일 팁 데이터 클래스
package com.galaxypods.companion.domain.tip

/**
 * "오늘의 팁" 항목.
 *
 * @property id 안정적 식별자 (분석/A-B 테스트용, v1.0에선 단순 라벨)
 * @property text 사용자에게 표시될 본문 (한국어)
 */
data class Tip(
    val id: String,
    val text: String,
)
