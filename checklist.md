# GalaxyPods 구현 체크리스트

> 진행 상황 추적용. 완료 시 `[x]`로 변경.
> 새 작업 발생 시 해당 섹션 하단에 추가.

---

## Phase 0 — 프로젝트 부트스트랩

- [x] CLAUDE.md 작성
- [x] checklist.md 작성
- [x] context-notes.md 작성
- [ ] Gradle 프로젝트 스켈레톤
  - [x] `settings.gradle.kts`
  - [x] `build.gradle.kts` (root)
  - [x] `gradle/libs.versions.toml`
  - [x] `gradle.properties`
  - [x] `app/build.gradle.kts`
  - [x] `app/src/main/AndroidManifest.xml`
  - [ ] `app/src/main/java/com/galaxypods/companion/GalaxyPodsApp.kt`
  - [ ] `app/src/main/res/values/strings.xml`
  - [ ] `app/src/main/res/values-ko/strings.xml`
  - [ ] `app/src/main/res/values/themes.xml`
- [x] `.gitignore`
- [x] `.editorconfig`
- [x] `.github/workflows/ci.yml`
- [ ] `gradle/wrapper/gradle-wrapper.properties` + jar (gradle wrapper 생성은 로컬 `gradle wrapper` 명령으로)
- [ ] 첫 커밋 + 원격 저장소 연결

---

## Phase 1 — BLE 파서 (코어, 가장 먼저)

- [x] `domain/model/AirPodsModel.kt` (AirPods + **Beats 11종** enum)
- [x] `domain/model/AirPodsAdvertisement.kt` (파싱 결과 데이터 클래스)
- [x] `data/ble/ParserConfig.kt` (오프셋 설정)
- [x] `data/ble/AirPodsModelTable.kt` (AirPods + Beats Device Type 룩업)
- [x] `data/ble/AppleContinuityParser.kt` (TLV 파서)
- [x] `app/src/test/resources/ble_dumps/README.md` (dump 수집 가이드)
- [x] `app/src/test/java/.../AppleContinuityParserTest.kt` (골든바이트 + Beats 식별 테스트)
- [ ] 실기기로 모델별 광고 dump 수집 (최소 5개 모델, AirPods + Beats 각 1개 이상)
- [ ] 골든바이트 테스트 모든 케이스 추가
- [ ] 파서 커버리지 90% 이상 확인

---

## Phase 2 — BLE 스캐너 + FGS

- [x] `domain/repository/PodsRepository.kt` (인터페이스)
- [x] `data/ble/BleScanner.kt` (ScanFilter 정밀화)
  - [x] `manufacturerId=0x004C` + `manufacturerData` 마스크 `[0x07, 0x19]` 헤더
  - [x] Active Scan (포그라운드, BALANCED + AGGRESSIVE)
  - [ ] PendingIntent Scan (백그라운드, 스로틀 면제) — Phase 2.5로 분리
  - [x] Android 12 이상/이하 권한 분기
- [x] `data/PodsRepositoryImpl.kt` (Scanner + Parser → StateFlow)
- [x] `service/PodsForegroundService.kt`
  - [x] `foregroundServiceType="connectedDevice"` (Android 14+)
  - [x] StateFlow → Notification 갱신
  - [x] 미디어 액션 4종 통합 (이전/재생·정지/다음/종료)
  - [x] VoiceAnnouncer 통합
  - [ ] 30초 주기 생존 신호 (현재는 광고 수신 시마다 갱신, 보강 가능)
- [x] `service/BootReceiver.kt` (BOOT_COMPLETED + MY_PACKAGE_REPLACED, 권한 사전 검사)
- [x] `di/AppModule.kt` (Hilt 모듈 + Repository Binds)
- [x] `presentation/MainActivity.kt` (디버그 화면 — Phase 3에서 본격 구현)
- [x] BLE 권한 흐름 (`BLUETOOTH_SCAN` neverForLocation, MainActivity 런타임 요청)
- [x] `app/src/test/.../PodsRepositoryImplTest.kt` (5개 케이스)
- [ ] 실기기 배터리 감지 검증 (사용자 액션)
- [ ] 실기기 절전 정책 24시간 안정성 테스트

---

## Phase 3 — 메인 화면 + 자동 재생/정지

- [x] `presentation/main/MainUiState.kt` (단일 진실 원천 데이터 클래스)
- [x] `presentation/main/MainViewModel.kt` (Repository 구독 + 토글 + FGS 트리거)
- [x] `presentation/main/MainScreen.kt` (Compose, 검토안 §6.2 레이아웃)
  - [x] ConnectionHeader (모델 아이콘 + 연결 상태)
  - [x] BatteryRow (좌/우/케이스 카드 3개)
  - [x] ToggleRow (귀감지 / 음성 안내)
  - [x] DisclaimerSection (Apple/Samsung 면책)
- [x] `presentation/theme/Theme.kt` (Material 3 + Dynamic Color API 31+ 폴백)
- [x] `presentation/MainActivity.kt` 본격 구현 (placeholder → MainScreen)
- [x] `data/system/MediaController.kt` (Phase 1.5에서 이미 작성)
- [x] `domain/usecase/AutoPlayPause.kt` (RELAXED_EITHER / STRICT_BOTH 모드 + 안전장치)
- [x] AutoPlayPause 단위 테스트 (10개 케이스)
- [x] in-ear 변화 감지 → 미디어 키 송신 (FGS observeRepository에서 트리거)
- [ ] 실기기 재생/정지 검증 (YouTube, Spotify) — 사용자 액션
- [ ] 모델 아이콘 디자인 리소스 교체 (현재는 displayName 첫 글자 placeholder)

---

## Phase 4 — 온보딩 + Samsung 절전 안내

- [x] `data/system/SamsungQuirks.kt` (One UI 버전 감지 + 딥링크 + sleep status 진단)
  - [x] `SemPlatformVersion` reflection (Samsung 비공식)
  - [x] SDK_INT 폴백 매핑 (Android 11~16 → One UI 3~8)
  - [x] One UI 6+ 딥링크 (`com.samsung.android.sm.ui.battery.BatteryActivity`, `activity_type=2`)
  - [x] 폴백 (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` → `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`)
  - [x] `isIgnoringBatteryOptimizations()` + `sleepStatus()` 자가진단
- [x] `data/preferences/AppPreferences.kt` (DataStore — 6개 키)
- [x] `presentation/onboarding/OnboardingState.kt` (단계 enum + UiState)
- [x] `presentation/onboarding/OnboardingViewModel.kt` (단계 진행 + 권한 결과 + 딥링크)
- [x] `presentation/onboarding/OnboardingScreen.kt` (Compose 5단계 마법사)
  - [x] Step 1. Welcome
  - [x] Step 2. Bluetooth 권한 (Android 12 ± 분기)
  - [x] Step 3. 알림 권한 (Android 13+ 조건부)
  - [x] Step 4. Samsung 절전 (One UI 버전별 안내, 비-삼성 자동 건너뜀)
  - [x] Step 5. 위치 (선택, opt-in 토글 + 권한)
  - [x] Step 6. Done (영속화 + 콜백)
  - [x] LinearProgressIndicator 단계 표시
- [x] `presentation/MainActivity.kt` (AppGateViewModel 분기, OnboardingScreen ↔ MainScreen)
- [x] `app/src/test/.../SamsungQuirksLogicTest.kt` (SDK 폴백 매핑 검증)
- [x] strings.xml 온보딩 문구 (한/영)
- [ ] 실기기 검증 (Samsung One UI 6+, 비-삼성 단말, 권한 거부 흐름) — 사용자 액션

---

## Phase 5 — 케이스 알림 + 위치

- [x] `domain/usecase/CaseOpenDetect.kt` (`lidOpenCount` 변화 감지 + 쿨다운 + 멱등)
- [x] `service/CaseOpenNotifier.kt` — **v1.0 풀스크린 알림** (HIGH 채널 + setFullScreenIntent)
- [x] `GalaxyPodsApp.kt` — case_open_alert 알림 채널 추가
- [x] `domain/model/LastLocation.kt` (도메인 모델)
- [x] `data/location/LastLocationStore.kt` (FusedLocation `getCurrentLocation` 1회 fetch, BG 위치 회피)
- [x] `data/preferences/AppPreferences.kt` 확장 (`lastLocation` Flow + setter/clear)
- [x] `presentation/location/LastLocationViewModel.kt`
- [x] `presentation/location/LastLocationScreen.kt` (Google Maps Compose + EmptyState + Detail + Clear)
- [x] PodsForegroundService 통합
  - [x] CaseOpenDetect → CaseOpenNotifier 호출
  - [x] CONNECTED → DISCONNECTED 전이 시 LastLocationStore.captureNow (옵트인 시)
- [x] AndroidManifest Maps API 키 메타데이터 (`${MAPS_API_KEY}` placeholder)
- [x] app/build.gradle.kts manifestPlaceholders (local.properties / env 주입)
- [x] `app/src/test/.../CaseOpenDetectTest.kt` (7개 케이스 — 기준선/멱등/쿨다운/감소/disabled/reset)
- [x] strings.xml 케이스 채널 + Maps 안내 (한/영)
- [ ] `local.properties`에 `MAPS_API_KEY=...` 등록 — 사용자 액션
- [ ] 실기기 케이스 오픈 알림 검증 (Android 14+ FullScreenIntent 권한 흐름) — 사용자 액션
- [ ] 실기기 위치 1회 fetch 검증 — 사용자 액션

---

## Phase 6 — 위젯 + 알림바

- [x] `domain/model/WidgetSnapshot.kt` (영속화 가능한 최소 필드 모델)
- [x] `data/preferences/AppPreferences.kt` 확장 (`widgetSnapshot` Flow + setter)
- [x] `presentation/widget/PodsAppWidgetProvider.kt` (Hilt-aware AppWidgetProvider)
- [x] `presentation/widget/AppWidgetUpdater.kt` (FGS → 영속화 + pushUpdate 통합)
- [x] `app/src/main/res/layout/widget_pods_battery.xml` (4x1 셀)
- [x] `app/src/main/res/drawable/widget_background.xml` (라운드 + 라이트/다크 색상)
- [x] `app/src/main/res/values/colors.xml`, `values-night/colors.xml`
- [x] `app/src/main/res/xml/widget_pods_info.xml` (appwidget-provider 메타)
- [x] AndroidManifest 위젯 receiver 등록
- [x] `service/BatteryIcon.kt` (동적 % 비트맵 IconCompat — AndroPods 차용)
- [x] PodsForegroundService 통합
  - [x] AppWidgetUpdater 호출 (광고 콜백마다)
  - [x] BatteryIcon으로 알림 smallIcon 동적 설정 (좌/우 중 낮은 쪽)
- [x] `app/src/test/.../BatteryIconTest.kt` (5개 케이스 — 정상/음수/오버플로/자릿수)
- [x] strings.xml 위젯 안내 (한/영)
- [ ] 위젯 미리보기 이미지 (`@drawable/widget_preview`) — 디자인 작업 필요
- [ ] 실기기 위젯 추가 → 배터리 표시 검증 — 사용자 액션

---

## Phase 7 — 차별화 기능 (S급)

### 검토안 §5 S급
- [x] Crashlytics 골격 + 옵트인 (`data/system/CrashReporter.kt`) — Firebase 활성화는 사용자 액션
- [x] 케이스 분실 알림 (`domain/usecase/CaseLostDetect.kt` + `service/CaseLostNotifier.kt`)
  - [x] Haversine 거리 계산
  - [x] 5분 대기 타이머 + 50m 임계값
  - [x] FGS 통합 (CONNECTED→DISCONNECTED 전이 시 예약, 재연결 시 취소)
- [x] Tip of the Day (`domain/tip/TipProvider.kt` + 15개 팁 + MainScreen TipCard)
- [ ] Edge Panel 위젯 (Samsung Edge SDK) — **v1.1 백로그로 이관** (Samsung 별도 등록 필요)
- [x] 개인정보처리방침 GitHub Pages 호스팅 (`docs/`)

### competitive-analysis §4 S급 (PodsLink/AndroPods 차용)
- [x] Beats 시리즈 지원 (모델 enum + 룩업 테이블)
- [x] 한국어 TTS 음성 안내 (`VoiceAnnouncer.kt`) — 메시지 결정 로직 골격 + 단위 테스트
- [x] 알림 액션 미디어 컨트롤 (`MediaController.kt` + `NotificationActionReceiver.kt`)
- [x] TTS 옵션 UI (설정 → 음성 안내 ON/OFF, 임계값 10/20/30%) — `SettingsScreen.kt`
- [x] FGS Notification에 미디어 액션 버튼 통합 (Phase 2에서 완료)
- [ ] Beats 모델별 아이콘 / 디자인 리소스 — 디자이너 작업 필요

### Phase 7 신규 화면 + 통합
- [x] `presentation/settings/SettingsViewModel.kt` (DataStore 6개 키 + UseCase 즉시 반영)
- [x] `presentation/settings/SettingsScreen.kt` (귀감지 모드/음성/위치/분실/Crashlytics/Samsung 절전)
- [x] MainScreen 라우팅 (HOME ↔ SETTINGS ↔ LOCATION 인메모리 분기)
- [x] `data/preferences/AppPreferences.kt` 확장 (caseLostAlertEnabled + crashlyticsOptIn)
- [x] `app/src/test/.../CaseLostDetectTest.kt` (7개 케이스 — Haversine 정확성 + 분기)

---

## Phase 8 — Play Store 출시 준비

### 코드/문서 (Claude 작업, 완료)
- [x] 스토어 메타데이터 (한국어 + 영어) — `store-listing/{ko-KR,en-US}/`
- [x] 권한 정당성 영상 스크립트 3편 — `store-listing/permission-justification-videos.md`
- [x] 개인정보처리방침 (한/영) — `docs/privacy-{ko,en}.md`
- [x] 이용약관 (한/영) — `docs/terms-{ko,en}.md`
- [x] Apple/Samsung 상표 면책 문구 — strings.xml + store-listing + privacy/terms §9 + AboutScreen
- [x] Data Safety Form 답변 매트릭스 — `store-listing/data-safety-form.md`
- [x] 데이터 삭제 안내 페이지 (한/영) — `docs/data-deletion-{ko,en}.md`
- [x] GitHub Pages Jekyll 설정 — `docs/_config.yml` + `.github/workflows/pages.yml`
- [x] **AboutScreen** (`presentation/about/AboutScreen.kt`) — 앱 내 면책 + 약관/개인정보/삭제 링크
- [x] BuildConfig URL 상수 (`PRIVACY_POLICY_URL`, `TERMS_URL`, `DATA_DELETION_URL`)
- [x] **CI release 빌드** (`.github/workflows/ci.yml` `release-bundle` job — 태그 푸시 시 AAB 생성)
- [x] **Compose Preview 5종** (`presentation/preview/ScreenshotPreviews.kt`) — 스크린샷 캡처 가이드
- [x] **RELEASE.md** — 출시 통합 가이드 (12개 섹션, 일정 권장 포함)

### 사용자 액션 (Claude 못 함)
- [x] 자리표시자 1차 치환 — 이메일 / 책임자 / 리포명 (Claude 처리 완료)
- [ ] **자리표시자 2차 치환** — GitHub username 결정 후 `yourname` → 본인 username (RELEASE.md §1.2 PowerShell 명령)
- [ ] 출시 직전 본인 정보 — 책임자 실명/사업자명 (privacy-{ko,en}.md §10)
- [ ] Google Maps API 키 발급 + `local.properties` 등록
- [ ] AAB 서명 키 (.jks) 생성 + 비밀번호 백업 + Play App Signing 위임
- [ ] Firebase 프로젝트 생성 + `google-services.json` 배치 (Crashlytics 활성화 시)
- [ ] GitHub 리포 생성 + Pages 활성화 (Settings → Pages → Source: GitHub Actions)
- [ ] CI Secrets 등록 (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `MAPS_API_KEY`)
- [ ] 스크린샷 5장 캡처 (Phone/Tablet/Foldable)
- [ ] 권한 정당성 영상 3편 촬영 + YouTube 일부 공개 업로드
- [ ] 앱 아이콘 디자인 (512×512 + 피처 그래픽 1024×500)
- [ ] 실기기 검증 매트릭스 (RELEASE.md §6)
- [ ] Play Console 등록 ($25) + AAB 업로드
- [ ] Internal → Closed → Open Beta → Production 단계 배포

### v1.0 실기기 검증 결과 (2026-05-18, 두 단말)

검증 단말. Galaxy S24 Ultra (Android 16 / One UI 8.0) + Galaxy Note 20 Ultra (Android 13 / One UI 5.0)

- [x] Application + Hilt + Compose 정상 동작 (두 단말)
- [x] Onboarding 5단계 흐름 정상
- [x] FGS 자동 시작 (MainScreen LaunchedEffect)
- [x] BLE 스캔 동작 (356+ Apple 광고 수신)
- [x] edge-to-edge + safeDrawingPadding (시스템 바 가려짐 X)
- [x] Theme.DeviceDefault 호환 (One UI 8 inflate 충돌 해결)
- [x] Type 0x10 Nearby Info 거짓 양성 방지
- [ ] **Type 0x07 (Proximity Pairing) 수신 — 신펌웨어 AirPods Pro에서 0건** ★
  - 두 단말 모두 동일. AirPods Pro 자체가 광고 안 함
  - v1.0은 구펌웨어 / AirPods 2/3 / AirPods Max에서만 배터리 표시
  - 신펌웨어 = v2.0+ AAP/L2CAP 필요

### 출시 전 마지막 점검 (사용자 액션 필요)

- [ ] `docs/_config.yml`의 `baseurl`을 실제 리포명으로 변경
- [x] 1차 치환 완료 (이메일/책임자/리포명) — RELEASE.md §1.1
- [ ] 2차 치환 — `yourname` → 본인 GitHub username — RELEASE.md §1.2
- [ ] 개인정보 보호책임자 연락처 기재 (`docs/privacy-ko.md` §10)
- [ ] `app_name`이 Play Console 등록명과 일치 확인

---

## Phase 9 — v1.1 / v2.0 백로그

### v1.1
- [ ] Edge Panel 위젯 (Samsung Edge SDK + Samsung 개발자 등록) — Phase 7에서 이관
- [ ] SYSTEM_ALERT_WINDOW 옵션 (Play 정책 통과 후)
- [ ] 케이스 팝업 커스터마이즈 (배경/위치/시간) — PodsLink 차용
- [ ] 모델별 동적 애니메이션 (Lottie / AnimatedVectorDrawable) — PodsLink 차용
- [ ] Voice Assistant 호출 알림 액션 (`MediaController.invokeVoiceAssistant()` 사용) — AndroPods 차용
- [ ] Bixby Routines 통합
- [ ] AOD 위젯 (One UI 6+)
- [ ] 카카오톡 알림 ducking 옵션
- [ ] 네이버 지도 SDK 통합 (Google Maps 대안)
- [ ] BLE RSSI 기반 Find My 가이드 (마지막 위치 적극형) — PodsLink 차용

### v2.0 — **AAP/L2CAP 채널 (실기기 검증으로 최우선 격상)**
- [ ] **Apple Accessory Protocol L2CAP CoC (PSM 0x1001) 채널 구현 — 최우선** ★
  - 신펌웨어 AirPods Pro의 Type 0x07 광고 미송출 한계 극복
  - 페어링 active 상태에서 실시간 배터리/in-ear/모델 정확 수신
  - LibrePods (GPLv3) 코드 직접 복사 금지 — AAP Definitions.md만 참조해 독자 구현
- [ ] Galaxy Watch 모듈 (IAP)
- [ ] 시스템 Equalizer 노출 (Audio Effects API) — PodsLink 차용
- [ ] 1% 단위 배터리 (AAP 채널 검토 → AAP로 자동 해결)
- [ ] 다중 기기 관리 (IAP)

### v2.0+
- [ ] ANC/투명도 전환 (L2CAP 안정화 시)
- [ ] 클라우드소싱 광고 패킷 데이터셋 (사용자 옵트인)
- [ ] L2CAP 우회 실험 (개발자 옵션)

---

## 일반 메모

- 완료 시 `[x]`로 변경 + 커밋 메시지에 항목명 포함
- 새 PR마다 detekt + ktlint + lint + test 4종 그린 필수
- 실기기 변경(BLE/FGS/오버레이/위치) PR은 실측 스크린샷·로그 첨부
