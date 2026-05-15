<!-- 면책 문구 표시 위치 체크리스트 -->
# Apple/Samsung 상표 면책 표시 위치

CLAUDE.md 원칙 10. "Apple/AirPods 상표는 UI에 직접 사용 금지. 도움말만 nominative fair use."

면책 문구는 다음 **모든** 위치에 일관되게 표시되어야 한다. 누락 시 Google Play 상표권 클레임 또는 Apple 직접 클레임의 방어 근거가 약해진다.

## 표시 위치 체크리스트

| # | 위치 | 한국어 텍스트 | 영어 텍스트 | 구현 상태 |
|---|---|---|---|---|
| 1 | Play Store 자세한 설명 본문 | `disclaimer_apple` 전문 | full disclaimer | ✓ store-listing/{locale}/full-description.txt |
| 2 | 앱 내 "정보(About)" 화면 | strings.xml `disclaimer_apple` | strings.xml `disclaimer_apple` | ✓ values/strings.xml + values-ko/strings.xml |
| 3 | 첫 실행 온보딩 마지막 단계 | "Apple Inc.와 무관함을 확인했습니다" 체크박스 | Same in English | ⏳ Phase 4에서 구현 |
| 4 | 개인정보처리방침 / 이용약관 §9 | 명시 조항 | 명시 조항 | ✓ docs/privacy-{lang}.md, docs/terms-{lang}.md |
| 5 | GitHub README | 면책 섹션 | 면책 섹션 | ⏳ 리포 생성 후 |
| 6 | 권한 정당성 영상 자막 | 영상 마지막 3초 면책 표시 | 영상 마지막 3초 면책 표시 | ⏳ 영상 촬영 시 |

## 강조 표현 (필수 단어)

면책 문구에 다음 단어가 모두 포함되어야 함.

- ✅ "무관" / "not affiliated"
- ✅ "Apple Inc." (정확한 법인명)
- ✅ "등록상표" / "registered trademark"
- ✅ "개발·승인·후원하지 않음" / "not developed, endorsed, or sponsored by"

## 금지 표현

- ❌ "Apple 공식" / "Apple Official"
- ❌ "Apple 추천" / "Apple Recommended"
- ❌ "공식 AirPods 앱" / "Official AirPods App"
- ❌ AirPods 형태를 정확히 모방한 아이콘
- ❌ Apple Logo 또는 Samsung Logo 사용

## UI 텍스트에서 "AirPods" 등장 횟수 자동 검증

CI에서 `app/src/main/res/values*/strings.xml`을 grep해 "AirPods", "Apple" 등장 횟수가 일정 기준을 넘으면 경고.
권장 임계값. 한 strings.xml 당 "AirPods" 5회 이하, "Apple" 면책 1회 + "AirPods" 단어 외 0회.

```bash
# CI 검증 예시
grep -c "AirPods" app/src/main/res/values/strings.xml  # < 5
grep -c "Apple" app/src/main/res/values/strings.xml    # 면책 문자열만, < 3
```
