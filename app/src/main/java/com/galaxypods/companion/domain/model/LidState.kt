// 케이스 뚜껑 상태 — CAPod LidState 정렬
package com.galaxypods.companion.domain.model

/**
 * AirPods 케이스 뚜껑 상태.
 *
 * **출처.** CAPod `DualApplePods.LidState` 정렬.
 * Continuity 광고의 lid open byte (offset 6) bit 3로 디코딩.
 * 단, case 컨텍스트(this pod in case / one pod / both pods)가 있을 때만 OPEN/CLOSED를
 * 구분하며, case 컨텍스트가 없으면 `NOT_IN_CASE`로 폴백.
 *
 * **글리치 주의.** CAPod 노트 — lid open counter는 누적 카운터이나 시간 지나면 reset 됨.
 * 빠르게 반복 open/close하면 counter 값이 변함. 본 LidState는 매 광고의 비트 3만 사용해
 * counter glitch와 무관.
 */
enum class LidState {
    /** 뚜껑 열림. */
    OPEN,

    /** 뚜껑 닫힘. */
    CLOSED,

    /** case 컨텍스트 없음 (pods 모두 귀에 있거나 케이스 신호 없음). */
    NOT_IN_CASE,

    /** 비트 해석 실패 (안전 폴백). */
    UNKNOWN,
    ;

    companion object {
        /**
         * lid byte의 bit 3 + case 컨텍스트로 LidState 결정.
         *
         * @param lidByte ParserConfig.lidOpenOffset 위치의 byte.
         * @param hasCaseContext this pod / one pod / both pods가 케이스에 있는지.
         */
        fun fromRaw(
            lidByte: Int,
            hasCaseContext: Boolean,
        ): LidState {
            if (!hasCaseContext) return NOT_IN_CASE
            return when ((lidByte shr 3) and 0x01) {
                0 -> OPEN
                1 -> CLOSED
                else -> UNKNOWN
            }
        }
    }
}
