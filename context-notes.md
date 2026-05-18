# GalaxyPods 의사결정 노트

> 구현 중 내려진 결정과 그 사유 누적. 미래의 자신·다른 세션이 같은 토론을 반복하지 않도록.
> 새 결정은 맨 위에 [날짜] 형식으로 추가.

---

## 2026-05-18 — Bluetooth Classic 연결 추적 + 파서 LE 수정 + 충전 false positive 차단

### 결정 56. BluetoothClassicMonitor 도입 — A2DP/HSP가 진짜 "연결됨" 신호

**왜.** BLE 광고는 페어링 활성 상태에서만 나옴. 케이스 닫음 ≠ 사용자가 해제했음일 수 있어 광고 끊김만으로 진짜 연결 해제 판단 불가. A2DP/HSP 프로필 연결 상태는 시스템 Bluetooth 스택이 직접 관리 → 진실 출처.

**구현.**
- `BluetoothClassicMonitor` (Singleton). `BluetoothManager.adapter.getProfileProxy(A2DP|HEADSET)` + BroadcastReceiver
- `BluetoothProfile.A2DP.connectedDevices` + `HEADSET.connectedDevices` → AirPods 식별 → StateFlow
- AirPods 식별. (1) AAP UUID `74ec2172-0bad-4d01-8f77-997b2be0722a` 보유 → 확정, (2) 폴백. 이름에 `airpods/pods/beats/powerbeats` 포함
- `PodsRepositoryImpl.startScanning`에서 1회 활성화. status는 Classic 모니터가 결정

**핵심 버그 두 가지 — 실측 검증 후 발견.**
1. **`RECEIVER_NOT_EXPORTED` 차단** — 시스템 브로드캐스트 못 받음. Android 14+ 정책으로 `RECEIVER_EXPORTED` 필요. NOT_EXPORTED는 같은 앱 내 브로드캐스트만 수신.
2. **브로드캐스트 누락 대응 폴링** — OEM 변형/타이밍 대응 위해 5초마다 강제 recompute 추가. 안전망.

**검증 (Note 20, 2026-05-18 11:09~11:10).**
```
recompute: 1 devices, airpods=민호의 AirPods Pro     ← 시작 시 인식
recompute: 1 devices, airpods=민호의 AirPods Pro     ← 5초 폴링
Connection event: HEADSET CONNECTION_STATE_CHANGED   ← 케이스 닫음 즉시 감지
recompute: 0 devices, airpods=<none>                  ← 0.5초 안에 끊김 반영
Connection event: ACL_DISCONNECTED
Connection event: A2DP CONNECTION_STATE_CHANGED
```

### 결정 57. Device Type 파서 little-endian 수정 (offset=1)

**왜.** 실측 캡처 `07 19 07 24 20 15 ...`에서 device type bytes가 `24 20` (LE) → 0x2024 (AIRPODS_PRO_2_USBC). 기존 파서가 BE로 읽어 0x0724 → UNKNOWN. 모델 테이블의 0x2024가 정답이었지만 파서가 못 찾음.

**수정.**
- `ParserConfig.deviceTypeOffset = 1` (prefix 바이트 스킵)
- `readDeviceType`을 LE: `(high << 8) | low` where `low=offset+0, high=offset+1`
- 테스트 `buildPayload`도 prefix + LE 배치로 업데이트
- 실측 캡처 패킷 골든 테스트 추가 (`parse_realCapture_airPodsPro2UsbcDuringRepair`)

### 결정 58. 충전 플래그 임시 비활성 (chargingDisabled=true)

**왜.** 실측 패킷에서 `chargingOffset=7`이 IRK 영역(랜덤 바이트)을 가리켜 비충전 상태에서도 false positive. 사용자가 "충전 중 아닌데 충전 중으로 나옴" 보고.

**임시 조치.** 파서가 항상 charging=false 반환. UI에 잘못된 "🔌 충전 중" 배지 표시 방지.

**TODO.** 알려진 충전 상태(케이스에 넣고 충전 vs 미충전)의 패킷을 비교해 정확한 byte 위치 + 비트 마스크 역추적 후 재활성.

### 결정 59. 배터리 실시간 갱신은 v2.0 영역 — 한계 명시

**확정 사실.**
- Type 0x07 (배터리 포함)은 페어링 핸드셰이크 순간에만 송출. 케이스 열기/닫기로 트리거 안 됨.
- 페어링 활성 후엔 Type 0x10 (Nearby Info)만 옴 — 배터리 정보 없음.
- Samsung 시스템 메타데이터도 AirPods 배터리 = null (`untethered_*_battery=null`)
- 실시간 배터리 갱신 = v2.0 AAP/L2CAP (PSM 0x1001) 필요 → 루트 의무 → v2-aap-research §1

**현재 UX 전략.**
- 마지막 페어링 때 받은 배터리값을 스냅샷으로 영구 보존 (결정 52)
- "마지막 확인. X분 전" 명시로 stale 분명히 표현

**v1.0 출시 정책 (재확정).** 솔직한 안내 + 구펌웨어 한정 동작. 신펌웨어는 페어링 시점 배터리 + 스냅샷 폴백.

---

## 2026-05-18 — Persistent Snapshot 폴백 도입 (PodsLink 패턴 차용)

### 결정 52. 마지막 스냅샷을 메인 화면에 폴백 표시

**왜.** 결정 45에서 확인했듯 AirPods Pro 신펌웨어가 Type 0x07을 사실상 송출 안 함 → `advertisement`가 대부분 시간 null. 사용자가 앱을 열 때마다 "이어폰을 찾는 중"만 보면 비루트 한계 안에서도 가능한 UX 정보(직전 케이스 뚜껑 열림 시 받은 배터리 %)를 손해 봄.

**구현**.
- `MainUiState`에 `lastSnapshot: WidgetSnapshot?` 추가
- `MainViewModel`에서 `AppPreferences.widgetSnapshot` Flow를 5-인자 vararg combine으로 결합
- `MainScreen`.
  - `ConnectionHeader`. 광고 없으면 스냅샷 모델명 사용 + "마지막 확인. X분 전" 라벨
  - `BatteryRow` / `BatteryCard`. 광고 없으면 스냅샷 배터리 표시 + `stale=true` → 텍스트 알파 0.6 + "🕒 이전 값"
  - `formatTimeAgo()`. 60초/60분/24시간/7일 단위 한국어 상대시간
- in-ear / charging은 stale일 때 false 고정 (오해 방지)

**비교 - 경쟁 앱 패턴**.
- PodsLink. 동일한 방식 (DB에 마지막 스냅샷 저장, 항상 표시)
- AndroPods. 비슷하지만 "마지막 본 지 N분" 표시 없음
- 우리 차별점. 명시적 stale 표시 + 타임스탬프 → 사용자가 정보의 freshness를 인지

**불변 보존**. `_advertisement.value` 자체는 여전히 Type 0x07 받았을 때만 갱신. 폴백은 UI 레이어 한정.

---

## 2026-05-18 — 첫 실기기 검증 (Galaxy S24 Ultra + Note 20 Ultra)

### 결정 45. 두 단말에서 검증 — 코드는 모두 정상, AirPods Pro 신펌웨어 한계 확인

**검증 단말**.
- Galaxy S24 Ultra (SM-S928N) — Android 16 (API 36) / One UI 8.0
- Galaxy Note 20 Ultra (SM-N986N) — Android 13 (API 33) / One UI 5.0

**기록된 사실**.
- ✅ Application + Hilt + Compose + Onboarding + MainScreen + FGS + BLE 스캔 모두 정상
- ✅ Apple 광고 다수 수신 (총 356+건)
  - Type 0x10 (Nearby Info): 274건
  - Type 0x12 (Find My): 59건
  - Type 0x16 (기타): 23건
- ❌ **Type 0x07 (Proximity Pairing) — 0건**
- AirPods Pro 신펌웨어 (iOS 17.5+)가 Type 0x07 광고를 사실상 송출 안 함

**확인된 한계**. v1.0 BLE-only 방식으로는 신펌웨어 AirPods Pro 배터리 정보 못 받음.
LibrePods / AndroPods / PodsLink 모두 같은 제약. **v2.0+ AAP/L2CAP 채널이 유일한 해결책**.

### 결정 46. v1.0 출시 정책 — 솔직한 안내 + 구펌웨어 한정 동작

UX 결정.
- 메인 화면. 광고 미수신 시 "이어폰을 찾는 중" 유지
- Type 0x10 (Nearby Info) 받아도 status 변경 X (거짓 양성 방지)
- 케이스 뚜껑 열기 안내 문구 (Type 0x07 송출 트리거)
- 스토어 설명에 "구펌웨어 AirPods Pro / AirPods 2/3, AirPods Max 동작" 명시

### 결정 47. v1.1 Theme inflate 충돌 해결 = DeviceDefault 기반

**왜.** Galaxy S24 Ultra (One UI 8.0)에서 `android:Theme.Material.Light.NoActionBar` 시 즉시 튕김. OEM theme override 시 attribute inflate 실패. `Theme.DeviceDefault.Light.NoActionBar`로 변경 → OEM 기본 따라가 호환성 최대.

### 결정 48. Edge-to-edge 명시 + safeDrawingPadding 필수

**왜.** compileSdk 35 + Android 15+에서 ComponentActivity는 edge-to-edge 자동 적용. 그러나 OnboardingScreen 등 Scaffold 없는 화면은 시스템 바 가려짐. 모든 화면에 `safeDrawingPadding()` 또는 Scaffold 사용 필수.

### 결정 49. BLE ScanFilter manufacturerId만 사용 (Type/Length 마스크 제거)

**왜.** AirPods Pro 신펌웨어 광고가 다양한 Type/Length 변종 (Type 0x10이 5/6/7바이트 등). 우리 마스크 `[0x07, 0x19]`가 너무 엄격해 진짜 0x07조차 일부 변종에서 거부. manufacturerId만 매칭 + 코드 레벨에서 Type 0x07 검증 → 안정.

### 결정 50. PodsRepositoryImpl model UNKNOWN은 status 변경 X

**왜.** Type 0x10 (Nearby Info) fallback이 광고 수신 시 status를 CONNECTED로 바꾸면 거짓 양성. 사용자가 "연결됨"으로 오해. AirPods 페어링 해제 + 케이스 닫힘에도 다른 Apple 기기 광고로 "연결됨" 표시.

**구체.** `if (parsed.model != UNKNOWN) { _status.value = CONNECTED }`. Type 0x10은 받지만 무시.

### 결정 51. ADB 진단 자동화 스크립트

`scripts/diagnose-usb.ps1` 도입. 한 줄 실행으로 6단계.
1. ADB devices (정규식 캡처)
2. 단말 정보 (Brand/Model/Android/One UI)
3. uninstall
4. install -r
5. logcat clear + app launch + 6초 대기
6. FATAL EXCEPTION + GalaxyPods 로그 자동 추출

향후 모든 실기기 검증에 사용.

---

## 2026-05-15 — Phase 8 출시 준비 (Claude 코드 측 완료)

### 결정 40. About 화면을 별도로 두고 면책을 항상 접근 가능하게

**왜.** Apple 상표권 클레임 방어의 핵심은 **"면책이 명확하고 접근하기 쉬움"**. 메인 화면 하단에만 두면 사용자가 스크롤 안 함 → Apple이 "사용자 혼동" 주장 가능. 별도 About 화면 + 정보 아이콘 (TopAppBar)으로 한 번에 접근.

**구체.** `AboutScreen.kt`에 Apple/Samsung 면책 카드 2개 + 약관/개인정보/삭제 링크 3개 + 데이터 처리 요약 4줄.

### 결정 41. URL 상수는 BuildConfig로 빌드 타임 주입

**왜.** GitHub username 등이 코드에 하드코딩되면 사용자별 fork 시 매번 수정. `gradle.properties` / `local.properties`로 빌드 타임 주입 → 코드 변경 X.

**구체.** `app/build.gradle.kts`에 `PAGES_BASE_URL` 그래들 속성 → `buildConfigField`로 노출. `BuildConfig.PRIVACY_POLICY_URL` 등 사용.

### 결정 42. CI release 빌드는 태그 푸시 시에만 (PR마다 X)

**왜.** AAB 빌드는 시간 + 시크릿 (keystore base64) 노출 비용. PR마다 실행 시 외부 기여자에게도 노출 위험. 태그 (`v1.0.0` 등) 푸시 또는 수동 트리거 (`workflow_dispatch`)로 제한.

**구체.** `.github/workflows/ci.yml`의 `release-bundle` job에 `if: startsWith(github.ref, 'refs/tags/v') || github.event_name == 'workflow_dispatch'`.

### 결정 43. Compose Preview를 별도 파일로 분리해 스크린샷 캡처 자동화 준비

**왜.** Play Store 스크린샷은 5장 필수. 실기기 캡처는 Bluetooth 신호 의존 + 시간 소모. Compose Preview로 외관 미리 검증 + 향후 Roborazzi/Paparazzi 같은 도구로 자동 캡처 전환 가능.

**구체.** `presentation/preview/ScreenshotPreviews.kt`에 5개 Preview (메인 4개 + Tip 카드 1개). 합성 데이터로 ViewModel/Bluetooth 의존 X.

**v1.1 계획.** Roborazzi 도입해 CI에서 스크린샷 자동 생성 + diff 검증.

### 결정 44. RELEASE.md를 단일 진실 원천으로 사용자 액션 통합

**왜.** 사용자 액션이 여러 문서에 흩어져 있으면 누락 발생 위험. 12개 섹션 1페이지로 통합 + 일정 권장 (1인 11주) 명시.

**구체.** Phase 8 사용자 액션 모두 RELEASE.md로 이전. checklist.md에서 RELEASE.md 참조.

---

## 2026-05-15 — Phase 7 차별화 기능 S급

### 결정 34. Crashlytics는 SDK 연결 X 골격만, **옵트인 기본 false**

**왜.** CLAUDE.md 원칙 11에 따라 사용자 데이터 외부 전송은 동의가 기본 아님. 신규 한국 사용자가 익숙한 옵트아웃 패턴이 아닌, **옵트인** 패턴 채택. 평점 4.5+ 유지에 도움.

**구체.** `CrashReporter`는 logcat 출력만 수행. Firebase 활성화 시점에 SDK 호출로 교체. `google-services.json` 없어도 빌드 가능.

### 결정 35. 케이스 분실 알림은 **5분 + 50m** 임계값으로 적극 차별화

**왜.** PodsLink/AndroPods 모두 "마지막 위치 표시"는 있지만 적극 알림은 없음. 우리는 끊김 후 5분 동안 단말이 50m 이상 멀어지면 자동 알림 → "지하철 두고 내림", "회의실 두고 나옴" 시나리오 정확 대응.

**구체.** `PodsForegroundService`가 CONNECTED→DISCONNECTED 전이 시 위치 A 저장 + 5분 delay 코루틴 → 위치 B fetch → Haversine 거리 비교. 재연결 시 즉시 취소.

### 결정 36. CaseLostDetect는 안드로이드 의존성 없는 순수 함수

**왜.** Android `Location.distanceTo()` 사용 시 단위 테스트가 instrumentation 필요. Haversine을 직접 구현해 JVM 단위 테스트만으로 검증 가능.

**검증.** 서울시청-광화문 약 1km 케이스로 정확성 확인. ±50m 오차 허용 (이 정도 정확도는 분실 판정에 충분).

### 결정 37. Tip of the Day는 **15개 로컬 리소스, 외부 서버 X**

**왜.** 클라우드 팁 시스템은 운영 부담 + 데이터 전송 ↑. 15개로 시작해 v1.1에서 추가. 날짜 기반 결정적 인덱스 → 같은 날엔 같은 팁, 외부 통신 0.

**향후.** 사용자 리텐션 측정 후 팁 갯수 확장 또는 카테고리화. v2.0에서 클라우드 동기 검토 (옵트인).

### 결정 38. SettingsScreen은 단일 화면에 모든 설정 + 라우팅은 인메모리 enum

**왜.** v1.0은 화면이 3개 (Home, Settings, LastLocation). Navigation Compose 도입 비용 > 이득. enum + when으로 단순 처리.

**재검토 트리거.** 화면 6개 이상 추가 시 Navigation Compose 도입 검토.

### 결정 39. Edge Panel을 v1.1로 이관

**왜.** Samsung Edge Panel은 별도 Samsung Developer 등록 + Edge SDK 검토 통과 필요. 일반 Galaxy 단말 모두에 적용되지 않음. v1.0 출시 일정 영향 vs 기대 효과 따져 보면 v1.1 이관이 합리적.

---

## 2026-05-15 — Phase 6 위젯 + 알림바 동적 아이콘

### 결정 29. 위젯은 별도 영속 스냅샷(`WidgetSnapshot`)으로 분리, 전체 광고 영속화 X

**왜.** `AirPodsAdvertisement`를 그대로 영속화하면 stale in-ear / charging / lidOpenCount가 사용자에게 오해 유발. 위젯에 의미 있는 필드만 분리 (모델/배터리/timestamp).

**구체.** `WidgetSnapshot.fromAdvertisement(ad)` 변환. AppPreferences에 5개 키만 저장.

### 결정 30. 위젯은 시스템 자동 갱신 30분 + FGS 광고 콜백마다 명시 갱신

**왜.** appwidget-provider의 `updatePeriodMillis`는 최소 30분(시스템 정책). FGS가 살아있는 동안 광고를 받을 때마다 명시 갱신해야 즉시 반영. 시스템 자동 갱신은 FGS 죽은 상태에서도 마지막 영속 스냅샷 표시 보장.

**구체.** `AppWidgetUpdater.onAdvertisement()`이 영속화 + `PodsAppWidgetProvider.pushUpdate()` 둘 다 호출.

### 결정 31. 알림바 동적 % 아이콘은 좌/우 중 낮은 쪽 표시

**왜.** AndroPods 차용. 사용자가 가장 신경 써야 할 정보는 "곧 죽을 쪽". 케이스 배터리는 보조 정보라 알림바 메인에는 좌/우만.

**구체.** `BatteryIcon.createPercentIcon(minOf(left, right))`. 둘 다 음수면 폴백 일반 아이콘.

### 결정 32. BatteryIcon은 256x256 비트맵 텍스트 합성 (PNG 11장 미리 만들기 X)

**왜.** 0~100 11단위 PNG (—, 10, 20, ..., 100)는 11개 리소스 추가 + 디자인 일관성 유지 부담. Canvas + drawText 동적 생성이 더 단순. 성능 영향 무시 가능 (알림 갱신 빈도 < 5초/회).

**한계.** 자릿수에 따라 글자 크기 분기 (1~2자리 70%, 3자리 50%). 향후 디자인 도구로 PNG 교체 가능.

### 결정 33. 위젯 클릭 → MainActivity 열기 (Settings 직접 열기 X)

**왜.** PodsLink 위젯은 Pro 잠금이라 클릭 액션이 IAP 페이지로 가는 경우 多 → 사용자 불만. 우리는 Settings 등 다른 화면으로 가지 않고 메인 화면 직접 진입 → "위젯 = 단순 정보 표시 + 앱 진입"으로 명확.

---

## 2026-05-15 — Phase 5 케이스 알림 + 마지막 위치

### 결정 23. 케이스 오픈은 풀스크린 알림으로 — SYSTEM_ALERT_WINDOW v1.1 보류 유지

**왜.** CLAUDE.md 원칙 12 (v1.0 SYSTEM_ALERT_WINDOW 비활성). Play 2024 정책 강화로 신규 등록 앱은 거의 통과 X. 풀스크린 알림은 동등한 사용자 경험을 제공하면서 정책 안전.

**구체.** `CaseOpenNotifier.showCaseOpenAlert()`이 `setFullScreenIntent(highPriority=true)` + IMPORTANCE_HIGH 채널 사용. Android 14+ FullScreenIntent 권한 미부여 시 시스템이 자동으로 heads-up 알림으로 폴백.

### 결정 24. CaseOpenDetect는 쿨다운 2초로 중복 트리거 방지

**왜.** BLE 광고는 1초 미만 간격으로 여러 번 수신될 수 있음. 같은 lidOpenCount는 idempotent 처리되지만, 사용자가 케이스를 빠르게 여닫으면 짧은 시간 내 카운트가 두 번 증가 가능. 알림 폭주 방지.

**값.** 2000ms — 사용자가 "방금 열었네"를 인지할 시간 + 다음 의도적 액션까지 충분.

### 결정 25. lidOpenCount 감소(롤오버 또는 새 케이스)는 트리거 X, 기준선 재설정

**왜.** 광고 카운트는 1바이트 (0~255) → 롤오버 가능. 또한 사용자가 새 케이스로 교체하면 카운트 0부터 시작. 두 경우 모두 "케이스 오픈"으로 판단하면 오탐. 단순 휴리스틱 — 감소 시 기준선만 갱신.

### 결정 26. 마지막 위치는 FGS 컨텍스트 안에서만 1회 fetch — BG 위치 권한 회피

**왜.** Play Console에서 `ACCESS_BACKGROUND_LOCATION`은 매우 엄격 심사. FGS 안에서 호출되는 1회 `getCurrentLocation()`은 "background"가 아니라 "foreground service" 컨텍스트로 분류 → BG 권한 불필요.

**구체.** `PodsForegroundService.handleConnectionTransition()`이 CONNECTED → DISCONNECTED 전이 1회만 트리거. `requestLocationUpdates` 절대 사용 X.

### 결정 27. Google Maps API 키는 local.properties / env 주입, 폴백 placeholder

**왜.** API 키는 절대 git 커밋 금지 (.gitignore에 local.properties 있음). 로컬 빌드는 local.properties의 `MAPS_API_KEY=...`로, CI는 환경변수로 주입. 누락 시 `MISSING_KEY` 폴백 → 빌드는 가능하나 지도는 표시 X.

**구체.** `app/build.gradle.kts`의 `manifestPlaceholders["MAPS_API_KEY"]`. `AndroidManifest.xml`의 `<meta-data android:value="${MAPS_API_KEY}"/>`.

### 결정 28. LastLocation은 DataStore Preferences에 도수형으로 저장 (Proto 사용 X)

**왜.** 단일 좌표 한 점만 저장. Proto 정의 비용 > 이득. `doublePreferencesKey` + `longPreferencesKey` + `floatPreferencesKey`로 충분.

**확장 시 재고.** "최근 N개 위치 히스토리" 같은 기능 도입 시 Proto 또는 별도 SQLite 검토.

---

## 2026-05-15 — Phase 4 온보딩 + Samsung 절전

### 결정 18. One UI 버전 감지는 reflection 우선, SDK_INT 폴백

**왜.** Samsung은 `Build.VERSION.SEM_PLATFORM_INT` (비공식) 필드로 정확한 One UI 버전을 노출. 그러나 reflection 실패 가능 (Samsung 로직 변경, 비-삼성 단말). 실패 시 `Build.VERSION.SDK_INT` 기반 추정 폴백 (Android 14 → One UI 6 등). 추정은 100% 정확하지 않지만 분기 결정에는 충분.

**구체.** `SamsungQuirks.oneUiMajorVersion()`이 두 경로를 시도. SemPlatformVersion 변환 공식. `(value/10000) - 9`.

### 결정 19. Onboarding 단계는 비-삼성 단말에서 Samsung 단계 자동 스킵

**왜.** OnePlus/Pixel 등 비-삼성 사용자도 본 앱 사용 가능 (CLAUDE.md 원칙 6 — 분기 진입 금지). 비-삼성 단말은 Samsung 절전 단계가 무의미하므로 자동 건너뜀.

**구체.** `OnboardingViewModel.goNext()`가 `NOTIFICATION_PERMISSION` → (isSamsung==false 시) → `LOCATION_OPTIONAL` 직행. goPrevious도 대칭.

### 결정 20. Onboarding 완료 시 in-memory 토글로 즉시 화면 전환

**왜.** DataStore 영속화는 비동기 → 영속화 완료 대기 시 사용자가 마지막 단계 "시작하기"를 눌러도 즉각 반응 X. UX 손상.

**구체.** `MainActivity.AppContent`에 `localBypass: MutableState<Boolean>` 추가. 콜백 시 즉시 true → MainScreen 전환. DataStore는 백그라운드에서 영속화. Activity 재시작 시에는 DataStore 값으로 결정.

### 결정 21. 위치 권한은 Onboarding에서도 옵트인 강제

**왜.** 검토안 §3.1 High 리스크 1 (Apple 상표권)과 함께 §3.2 Mid 리스크 3 (Data Safety)에 직결. "기본 OFF + 사용자 명시 ON 시에만 권한 요청" 패턴이 Play Console 심사 안전.

**구체.** Step 5 LOCATION_OPTIONAL에서 사용자가 토글 ON 후 → 권한 요청 버튼 노출. 토글 OFF면 권한 요청 자체 없음.

### 결정 22. Samsung 딥링크는 try/catch + 표준 폴백 2단계

**왜.** `com.samsung.android.sm.ui.battery.BatteryActivity`는 비공식 Activity → One UI 마이너 업데이트로 변경 가능. 첫 시도 실패 시 표준 `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (앱 패키지 지정), 그것도 실패 시 `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (전체 목록).

**구체.** `SamsungQuirks.openBatteryOptimizationSettings()`가 3단계 폴백 체인.

---

## 2026-05-15 — Phase 3 메인 화면 + 자동 재생/정지

### 결정 13. AutoPlayPause 기본 모드는 RELAXED_EITHER

**왜.** "한쪽이라도 빼면 정지"가 일상 사용에서 가장 직관적. 통화 받으려 한쪽만 뺄 때, 음악 일시 멈추는 게 사용자 기대. STRICT_BOTH는 "양쪽 다 빼야 정지"라 한쪽 빼고 듣다가 음악이 계속 흘러나오는 어색함 발생.

**옵션 노출.** 설정 화면에서 사용자가 모드 선택 가능. AndroPods/PodsLink는 모드 옵션이 없거나 빈약 → 차별화.

### 결정 14. 자동 재생은 "우리가 정지한 후"에만 트리거

**왜.** 사용자가 본인 의지로 음악을 정지한 상태에서, 이어폰 끼는 동작만으로 재생되면 의도와 맞지 않음. `wasAutoPaused` 플래그로 우리가 정지시킨 경우에만 재생.

**한계.** 외부 앱(예: Spotify)에서 사용자가 정지한 시점은 우리가 알 수 없음. 휴리스틱으로 보완 가능하지만 v1.0은 단순 유지.

### 결정 15. ViewModel은 ApplicationContext만 받고 Activity Context 사용 X

**왜.** ViewModel이 Activity Context를 가지면 메모리 누수. FGS 시작/중지는 ApplicationContext로 충분 (Service intent 발행만 함).

**구체.** `MainViewModel`에 `@ApplicationContext` 주입. Activity 회전/재생성에도 ViewModel 안전.

### 결정 16. Compose 테마는 Dynamic Color (API 31+) + 정적 폴백

**왜.** Material You는 Galaxy 사용자가 OS 색상 (One UI 개인화)에 익숙해 친숙함 ↑. minSdk 26이라 폴백 정적 색상 필수. `dynamicColor` 매개변수로 명시 옵션 (테스트 격리 가능).

### 결정 17. 모델 아이콘은 v1.0 displayName 첫 글자 placeholder

**왜.** 디자인 리소스 작업 외주/직접 작업에 시간 필요. 출시 전 교체 전제로 placeholder 사용. 빌드 가능 + UI 흐름 검증 가능.

**TODO.** Phase 8 출시 준비에서 실제 아이콘 교체 (`presentation/theme` 또는 `presentation/components/ModelIcon.kt`).

---

## 2026-05-15 — 경쟁 앱 차용 결정 (PodsLink / AndroPods 비교 후)

### 결정 8. v1.0에 Beats 시리즈 11종 추가

**왜.** PodsLink가 100만+ 다운로드 중인 핵심 사유 중 하나가 Beats 지원. Beats는 한국에서도 인기. Apple Continuity 광고 포맷이 동일(0x004C / Type 0x07)하므로 **현재 파서 그대로 동작**, 추가 비용은 enum + 룩업 키만.

**구체.**
- `AirPodsModel.kt`에 `Category.BEATS` 추가 + 11개 모델 enum
- `AirPodsModelTable.kt`에 11개 Device Type 키 추가 (출처. CAPod Wiki + LibrePods)
- 일부 키(Beats Solo 4, Studio Buds+)는 OSS 합의 추정 → 실측 dump 필요

**시장 효과.** 잠재 사용자 약 1.5~2배 증가.

### 결정 9. 한국어 TTS 음성 안내 v1.0 포함 (S급 승격)

**왜.** PodsLink "Voice Broadcasting"이 사용자 만족도 항목. 운전·시각 접근성 시나리오에서 강력. 검토안 §5에서 B급이었으나 시장 데이터 기반 S급 승격.

**구체.** `data/system/VoiceAnnouncer.kt` 골격 생성. 메시지 결정 로직(케이스 오픈 / 임계값 도달) + Android `TextToSpeech` 통합. 사용자 옵트인 + 임계값(10/20/30%) 선택.

**기본값.** 비활성. 사용자가 명시적으로 켜야 함 (시끄러움 회피).

### 결정 10. 알림 액션 미디어 컨트롤 v1.0 포함

**왜.** PodsLink 제스처 / AndroPods Voice Assistant 패턴 통합. 사용자가 알림에서 직접 재생/정지/다음/이전을 누를 수 있어 UX 향상.

**구체.** `MediaController.kt` (시스템 미디어 키 dispatcher) + `NotificationActionReceiver.kt` (BroadcastReceiver). FGS Notification 빌더에서 액션 버튼 부착.

**Voice Assistant 호출.** `KEYCODE_VOICE_ASSIST`로 Google Assistant/Bixby 호출. v1.0 코드는 준비, UI 노출은 v1.1 (풀스크린 알림으로 1차 출시이므로 액션 버튼 노출 위치 재설계 필요).

### 결정 11. 위젯·귀감지·TTS·미디어 컨트롤 영구 무료 (CLAUDE.md 원칙 13 격상)

**왜.** PodsLink 위젯 IAP 잠금, AndroPods 귀감지 IAP 잠금이 사용자 평점 핵심 불만. GalaxyPods는 이를 무료로 풀어 차별화 + 평점 우위.

**IAP 한정 영역.** v2.0+ Galaxy Watch 모듈, 테마 팩, 다중 기기 관리. **핵심 기능에는 IAP 사용 금지.**

### 결정 12. AirPodsModel enum 이름 유지 (Beats 포함하지만 리네임 안 함)

**왜.** "AirPodsModel"이 Beats를 포함하면 명칭 모호하지만, 클래스 리네임은 import 경로 전체 영향. 현재 단계에서 비용 대비 이점 작음.

**대안.** `Category` enum으로 분류 명확화. 향후 "PodsModel"로 리네임 필요 시 일괄 리팩터링 PR로 처리.

**재검토 트리거.** 비-Apple 광고 포맷을 사용하는 모델(Galaxy Buds, Sony 등)이 추가될 때.

---

## 2026-05-15 — Phase 0 부트스트랩

### 결정 1. CLAUDE.md를 검토안 보강 사항으로 강화 (설계안 §14 단순 복사 X)

**왜.** 검토안에서 도출된 5가지 핵심 리스크(상표권, SYSTEM_ALERT_WINDOW, IRK 회전, ScanFilter 정밀화, GPL 격리)가 설계안 §14에 부분만 반영됨. CLAUDE.md는 매 세션 자동 로드되므로, Claude가 매번 같은 실수를 반복하지 않도록 12개 핵심 원칙으로 격상.

**구체.**
- 원칙 5. ScanFilter 정밀화 추가
- 원칙 12. SYSTEM_ALERT_WINDOW v1.0 비활성 (검토안 §3.1)
- 원칙 11. Crashlytics 옵트아웃 명시

### 결정 2. `SYSTEM_ALERT_WINDOW` v1.0 출시 시 비활성

**왜.** Google Play 2024년 정책 강화로 신규 등록 앱은 "core functionality only" 매우 엄격. AndroPods/MaterialPods가 통과한 선례는 있으나 신규는 더 엄격. 첫 출시 차단되면 7~14일 재심사 사이클 + 평점 회복 어려움.

**대안.** 풀스크린 알림(`USE_FULL_SCREEN_INTENT`)으로 1차 출시 → 사용자 피드백 누적 후 v1.1에 정당성 입증해 오버레이 옵션 추가.

**영향.** UX 손실. 그러나 출시 차단 손실보다 작음.

### 결정 3. `minSdk 26` 유지 (31로 올리지 않음)

**왜.** 검토안에서 31 권장했으나, 한국 시장 노년/저가 단말 비중 고려 시 시장 손실 10~15% 무시 못 함. 또한 Compose는 21+, Material 3 Dynamic Color만 31+ → API 31+에서만 Dynamic Color 적용 분기 처리.

**조건.** Android 8~10에서 BLE Scan API 변형 대응 코드 추가 부담 수용. `BleScanner.kt`에서 SDK_INT 분기 명시.

**재검토 트리거.** 출시 후 ANR/Crash이 Android 10 이하에 집중되면 minSdk 31 상향 검토.

### 결정 4. 외부 BLE 라이브러리 사용 금지 (Android Framework 직접)

**왜.** Nordic BLE Library / RxAndroidBle 등은 GATT 중심. 우리는 GATT 연결 안 함, 광고 패킷 파싱만. 라이브러리 의존은 APK 크기 + 추적 코드 부담만 증가.

**예외 없음.** 직접 `BluetoothLeScanner` + `ScanCallback`/`PendingIntent` 사용.

### 결정 5. CAPod 키 테이블은 `assets/airpods_keys.json`에 분리

**왜.** CAPod는 GPLv3. 코드 직접 복사 금지지만, Apple 펌웨어 산물인 키 값(Device Type 바이트)은 "사실 데이터(facts)" 항변 가능. 단, 코드와 섞이면 라이선스 오염 우려 → JSON 데이터 파일로 격리.

**구현.** `AirPodsModelTable.kt`는 JSON 로딩만, 키 자체는 데이터 파일.
실제 키는 첫 PR에서 추가하지 않음. 모델 식별 누락은 `UNKNOWN`으로 폴백.

### 결정 6. `gradle.properties` JVM args 최소화

**왜.** CI 실행 환경(GitHub Actions Ubuntu Runner)은 7GB RAM. `org.gradle.jvmargs=-Xmx4096m` 이상은 OOM 위험.

**설정.** `-Xmx2048m -XX:MaxMetaspaceSize=512m`.

### 결정 7. 첫 커밋은 사용자 명시 후

**왜.** 글로벌 CLAUDE.md §9는 "의미있는 단위로 커밋"이지만, "사용자 명시 없이는 커밋 금지" 원칙이 시스템 프롬프트에 있음. Phase 0 완료 후 사용자 승인 받고 커밋.

---

## 미해결 질문 (사용자 확인 필요)

1. **GitHub 리포지토리 이름 / 가시성**. `galaxy-pods` public? private?
2. **Firebase 프로젝트 생성 권한**. Crashlytics 통합 시점.
3. **Play Console 계정 보유 여부**. $25 일회 등록비.
4. **테스트용 AirPods 모델**. 어떤 모델 보유? 골든바이트 dump 수집 가능 모델 목록 필요.
5. **개발 단말**. 어떤 Galaxy 단말 보유? One UI 버전?
6. **기간**. 출시 목표 시기? (일정 산정 영향)

---

## 참고 — 자주 잊는 것

- Gradle wrapper jar는 `gradle wrapper` 명령으로 생성. 본 세션에서 직접 생성 X.
- BLE 광고 dump는 `nrf Connect` 또는 `BLE Scanner` 앱으로 수집. 16진수 ASCII로 저장.
- `targetSdk` 36 강제 시작은 2026-08. 출시 직전에 마이그레이션.
- AirPods는 BD_ADDR 무작위 회전. 동일 기기 추적은 lidOpenCount + 배터리 패턴 휴리스틱 (Phase 1에서 구현).
