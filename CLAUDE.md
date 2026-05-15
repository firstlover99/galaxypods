# CLAUDE.md — GalaxyPods 프로젝트 지침

> Claude Code에 항상 로드되는 프로젝트 핵심 지침. 사용자 글로벌 `~/.claude/CLAUDE.md`와 함께 적용된다.
> 충돌 시 글로벌 지침이 우선한다.

---

## 0. 프로젝트 한 줄 요약

Galaxy(One UI 3~8) 단말에서 AirPods 사용자가 iOS 수준 기본 경험(배터리 표시·케이스 팝업·귀감지 자동정지·자동연결·마지막 위치)을 무료로 누릴 수 있게 하는 비루트 컴패니언 앱. Play Store 무료 공개 배포 대상.

상세 설계는 `AirPods_Galaxy_앱_상세설계안.md.docx` 및 검토안 `~/.claude/plans/c-galaxypods-airpods-galaxy-md-docx-glimmering-coral.md` 참조.

---

## 1. 빌드 / 테스트 / 정적 분석

```bash
./gradlew assembleDebug          # 디버그 APK
./gradlew assembleRelease        # 릴리스 AAB
./gradlew test                   # JVM 단위 테스트
./gradlew connectedAndroidTest   # 실기기/에뮬레이터 instrumentation 테스트
./gradlew detekt                 # 정적 분석
./gradlew ktlintCheck            # 코드 스타일
./gradlew lint                   # Android Lint
```

CI 게이트(`.github/workflows/ci.yml`).
- PR. detekt + ktlint + lint + test (4개 모두 초록이어야 머지 가능)
- main push. 위 + assembleDebug + Pre-launch Report 트리거

---

## 2. 핵심 원칙 (Top 12)

1. **`AppleContinuityParser`는 앱 코어와 완전 분리**. Apple 펌웨어 변경 시 파서만 교체. `data/ble/AppleContinuityParser.kt` 단일 파일에 모든 비트 오프셋 격리.
2. **모든 파서 수정 시 골든바이트 단위 테스트 필수 갱신**. `app/src/test/resources/ble_dumps/` 의 실측 dump 사용. 파서 변경 PR은 테스트 변경 동반 필수.
3. **LibrePods(GPLv3) / CAPod 코드 직접 복사 금지**. AAP Definitions 문서·PoPETs 2020 논문만 1차 자료로 참조해 독자 구현. 키 테이블은 사실 데이터(facts)로 `assets/airpods_keys.json`에 분리.
4. **`BLUETOOTH_SCAN`에 항상 `neverForLocation` 플래그 유지**. 위치 정보 사용 안 함을 매니페스트·Data Safety·스토어 설명에 일관 명시.
5. **`ScanFilter` 정밀화 필수**. `manufacturerId=0x004C` 단독 금지. 반드시 `manufacturerData` 마스크에 `[0x07, 0x19]` (Type=0x07, Length>=25) 헤더 매칭 추가.
6. **Samsung `Build.MANUFACTURER` 감지로 One UI 전용 분기**. `data/system/SamsungQuirks.kt` 단일 파일에 격리. 비-삼성 단말은 분기 진입 금지.
7. **ANC/L2CAP 코드는 `isL2capSupported()` 플래그로 영구 비활성**. v1.0 출시 차단 사유. 코드 잔존 허용하되 호출 경로 차단.
8. **외부 의존성 추가 시 반드시 `gradle/libs.versions.toml`에 등록 + PR 사유 기재**. 직접 `implementation("...")` 금지.
9. **빌드/테스트 실패 시 1회 재시도 후 중단·보고**. 무한 재시도 금지.
10. **Apple/AirPods 상표는 UI에 직접 사용 금지**. 도움말 텍스트만 nominative fair use. 앱명·아이콘·스플래시·런처에 "AirPods" 문자열 금지. 스토어 짧은설명도 `Pods Companion` 등 우회 표현 우선.
11. **사용자 데이터 서버 전송 절대 금지**. 위치·배터리·페어링 이력 모두 온디바이스 DataStore. Crashlytics는 사용자 데이터 제외 + 옵트아웃 옵션 제공.
12. **`SYSTEM_ALERT_WINDOW`는 v1.0 출시 시 기본 비활성**. 풀스크린 알림으로 1차 출시 → 사용자 피드백 후 v1.1에서 옵션으로 제공. Play 정책 리스크 회피.
13. **위젯·귀감지·TTS 음성안내·미디어 컨트롤은 영구 무료**. 경쟁 앱(PodsLink/AndroPods)이 IAP로 잠근 기능을 무료로 풀어 차별화. IAP는 v2.0+ Galaxy Watch 모듈/테마 팩에 한정.
14. **Beats 시리즈 동등 지원**. `AirPodsModel.Category.BEATS` 모델은 AirPods와 동일한 코드 경로로 처리. 별도 분기 만들지 말 것 (광고 포맷 동일).
15. **최신 모델 출시 즉시 대응**. AirPods 신모델 또는 Beats 신모델 발표 시 1주 이내 Device Type 키 추가 PR 우선순위 최상.

---

## 3. 디렉토리 구조

```
:app
├── presentation/
│   ├── main/          # 메인 화면 (배터리, 연결 상태)
│   ├── popup/         # 케이스 오픈 오버레이 (v1.1 활성화)
│   ├── widget/        # 홈화면 위젯
│   ├── onboarding/    # 권한 요청, 절전 안내 마법사
│   ├── settings/      # 앱 설정
│   └── location/      # 마지막 위치 지도 화면
├── domain/
│   ├── model/         # AirPodsAdvertisement, AirPodsModel, PodsUiState
│   ├── usecase/       # AutoPlayPause, CaseOpenDetect, LocationRecord
│   └── repository/    # PodsRepository (인터페이스)
├── data/
│   ├── ble/
│   │   ├── AppleContinuityParser.kt    # 핵심 파서 (오프셋 격리)
│   │   ├── ParserConfig.kt             # 펌웨어 변경 대응
│   │   ├── AirPodsModelTable.kt        # 모델 식별 룩업
│   │   └── BleScanner.kt               # ScanFilter 정밀화
│   ├── system/
│   │   ├── MediaController.kt          # AudioManager 미디어 키
│   │   ├── OverlayManager.kt           # WindowManager (v1.1)
│   │   └── SamsungQuirks.kt            # One UI 분기
│   ├── location/
│   │   └── LastLocationStore.kt        # FusedLocation + DataStore
│   └── preferences/
│       └── AppPreferences.kt           # DataStore Preferences
├── service/
│   └── PodsForegroundService.kt        # connectedDevice FGS
└── di/
    └── AppModule.kt                    # Hilt 모듈
```

핵심 파일 위치(빠른 참조).
- BLE 파서. `app/src/main/java/com/galaxypods/companion/data/ble/AppleContinuityParser.kt`
- 골든바이트 dump. `app/src/test/resources/ble_dumps/{model}.bin`
- FGS. `app/src/main/java/com/galaxypods/companion/service/PodsForegroundService.kt`
- Samsung 분기. `app/src/main/java/com/galaxypods/companion/data/system/SamsungQuirks.kt`

---

## 4. 기술 스택 고정

| 구분 | 선택 |
|---|---|
| 언어 | Kotlin 2.x |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| 비동기 | Coroutines + Flow (StateFlow) |
| 영속성 | DataStore Preferences |
| BLE | Android Framework 직접 (외부 라이브러리 금지) |
| 빌드 | Gradle 8.x, AGP, Version Catalog |
| 정적 분석 | detekt + ktlint + Android Lint |
| 테스트 | JUnit5 + MockK + Compose UI Test |
| CI | GitHub Actions |
| 크래시 | Firebase Crashlytics (옵트아웃) |

`minSdk 26` (Android 8.0) 설정. Material 3 Dynamic Color는 API 31+ 폴백 필수.
`targetSdk 35`. 2026-08 전 `targetSdk 36` 마이그레이션 필수.

---

## 5. 산출물 의무 (사용자 글로벌 §7)

비자명한 작업 시작 전 다음 3종 항상 동기화.
- **Plan**. 이 CLAUDE.md + 검토안 plan 파일.
- **Checklist**. `checklist.md` (체크박스로 진행 추적).
- **Context Notes**. `context-notes.md` (의사결정 + 사유 누적).

신규 소스 파일 첫 줄에 한국어 한 줄 코멘트 의무 (글로벌 §6).

---

## 5.5 경쟁 앱 차용 결정 매트릭스 (competitive-analysis.md 요약)

v1.0 포함 (구현됨/구현 예정).
- ✅ Beats 시리즈 (Solo/Pro/Studio/Fit/Powerbeats) — `AirPodsModelTable.kt`
- ✅ 한국어 TTS 음성 안내 — `data/system/VoiceAnnouncer.kt`
- ✅ 알림 액션 미디어 컨트롤 — `data/system/MediaController.kt` + `service/NotificationActionReceiver.kt`

v1.1 백로그.
- ⏳ 모델별 동적 애니메이션 (Lottie / AnimatedVectorDrawable)
- ⏳ 케이스 팝업 커스터마이즈 (SYSTEM_ALERT_WINDOW 활성화 시점)
- ⏳ Voice Assistant 호출 알림 액션 (이미 `MediaController.invokeVoiceAssistant()` 준비됨)

v1.1+ / v2.0 백로그.
- ⏳ 시스템 Equalizer (Android Audio Effects API)
- ⏳ BLE RSSI 기반 Find My 가이드 (마지막 위치의 적극형)

회피 결정 (경쟁 약점 회피).
- ❌ 위젯 IAP 잠금 — PodsLink 사용자 평점 핵심 불만
- ❌ 귀감지 IAP 잠금 — AndroPods 무료 사용자 핵심 불만
- ❌ 최신 모델 늦게 지원 — AndroPods 결정적 약점

---

## 6. Play Store 출시 차단 요소 (출시 전 반드시 해결)

- [ ] 개인정보처리방침 GitHub Pages 호스팅 (한국어 + 영어)
- [ ] 이용약관
- [ ] Apple 상표 면책 문구 (앱 정보 + 스토어 설명)
- [ ] Data Safety 신고 (위치 = 수집, 제3자 공유 없음, 온디바이스)
- [ ] 권한 정당성 영상 (`SYSTEM_ALERT_WINDOW`, FGS, 위치)
- [ ] AAB 서명 키 안전 보관 (Play App Signing 위임 권장)
- [ ] Internal → Closed → Open Beta → Production 단계 배포

---

## 7. 의사결정 빠른 참조

- "이거 추가해도 돼?" → 검토안 §5(추가 아이디어) 우선순위 확인. S급 v1.0, A급 v1.1, C급 v2.0+.
- "이 권한 더 필요한데?" → 글로벌 §3(외과적 변경) + 본 §11(원칙 11) 위반 여부 먼저.
- "ANC/투명도 켜고 싶어" → §2(원칙 7) 위반. v2.0+ 영역.
- "외부 라이브러리 쓰자" → §2(원칙 8) 따라 PR 사유 + libs.versions.toml 등록.
- "실기기 없는데 PR 머지해도 돼?" → BLE/FGS/오버레이 변경 PR은 실기 검증 첨부 필수. 그 외는 CI 그린이면 OK.
