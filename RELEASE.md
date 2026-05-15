# GalaxyPods v1.0 출시 가이드

> v1.0 Production 배포까지 사용자가 직접 처리해야 할 모든 단계.
> Claude가 작성한 코드/문서는 모두 완료. 본 문서는 사용자 액션 통합 체크리스트.

---

## 0. 사전 준비 (1회성)

- [ ] **Google Play Console 계정** 등록 ($25 일회성 결제)
- [ ] **Firebase 프로젝트** 생성 (Crashlytics 활성화 시 필요, 선택)
- [ ] **Google Cloud Console** Maps SDK for Android 활성화 + API 키 발급
- [ ] **GitHub 계정** + 리포 생성 (Pages 호스팅용)
- [ ] **개인 도메인** (선택, `galaxypods.app` 등 ~$15/년)

---

## 1. 자리표시자 치환

### 1.1 1차 치환 (Claude가 처리 완료 — 2026-05-15)

다음 항목은 모든 파일에서 일괄 치환 완료.

| 항목 | 적용 값 |
|---|---|
| 연락처 이메일 | `galaxypods.support@gmail.com` |
| 리포명 | `galaxypods` (이미 모든 문서가 사용 중) |
| 책임자 이름 (한국어) | `GalaxyPods 운영자` (실명/사업자명은 출시 직전 본인 정보로 교체) |
| 책임자 이름 (영문) | `GalaxyPods Operator` (동일) |

치환 대상 파일.
- `docs/privacy-ko.md` §10
- `docs/privacy-en.md` §10
- `docs/data-deletion-{ko,en}.md`
- `docs/index.md`
- `store-listing/{ko-KR,en-US}/full-description.txt`
- `store-listing/data-safety-form.md`

### 1.2 2차 치환 (사용자 액션 필요) — GitHub username 결정 후

GitHub username을 결정한 후 다음 PowerShell 명령으로 일괄 치환.

```powershell
# 1) 본인 GitHub username 입력
$username = "여기에_본인_GitHub_username"

# 2) docs/, store-listing/ 모든 파일에서 yourname.github.io → username.github.io 치환
Get-ChildItem -Path C:\GalaxyPods\docs, C:\GalaxyPods\store-listing `
  -Recurse -Include *.md, *.txt |
  ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $updated = $content -replace 'yourname\.github\.io', "$username.github.io"
    if ($content -ne $updated) {
      Set-Content $_.FullName -Value $updated -Encoding UTF8 -NoNewline
      Write-Host "Updated: $($_.FullName)"
    }
  }
```

또는 빌드 시점에 환경변수로 주입 (영구 치환 회피).
`local.properties`에 다음 추가.
```properties
MAPS_API_KEY=AIza...본인키
PAGES_BASE_URL=https://본인username.github.io/galaxypods
```

이러면 `app/build.gradle.kts`가 자동으로 BuildConfig에 주입 → AboutScreen 링크가 정확히 동작. 단, 스토어 리스팅 텍스트는 별도 치환 필요.

### 1.3 출시 직전 본인 정보로 교체

- [ ] `docs/privacy-ko.md` §10 — `GalaxyPods 운영자` → 실명 또는 사업자명
- [ ] `docs/privacy-en.md` §10 — `GalaxyPods Operator` → real name or company name

### 1.4 잔여 placeholder 검증 명령

```powershell
Select-String -Path C:\GalaxyPods\docs\*, C:\GalaxyPods\store-listing\* `
  -Pattern "yourname|TBD|TBA|MISSING_KEY|\[your-|\[개발자|\[Developer" -Recurse
```

위 명령에서 출력이 없으면 모든 자리표시자가 실제 값으로 치환된 것.

---

## 2. 키 / 인증서

### Google Maps API 키
1. Google Cloud Console → 새 프로젝트 또는 기존
2. Maps SDK for Android 사용 설정
3. 사용자 인증 정보 → API 키 만들기
4. **애플리케이션 제한**. Android 앱 → 패키지명 `com.galaxypods.companion` + SHA-1 핑거프린트
5. **API 제한**. Maps SDK for Android만 허용
6. `local.properties`에 `MAPS_API_KEY=...` 등록

### AAB 서명 키 (한 번만, 평생 보관)
```bash
# JKS 키스토어 생성
keytool -genkey -v -keystore galaxypods-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias galaxypods
```

- [ ] **`galaxypods-release.jks`를 안전한 곳에 백업** (잃으면 앱 업데이트 불가)
- [ ] 비밀번호도 별도 보관 (1Password / Bitwarden 권장)
- [ ] **Play App Signing 위임** — Play Console에서 업로드 키만 관리하도록 설정 (분실 위험 ↓)

### `app/build.gradle.kts` 서명 설정 활성화
```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_PATH") ?: "../keystore.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        ...
    }
}
```

---

## 3. Firebase Crashlytics 활성화 (선택)

- [ ] Firebase Console → 프로젝트 만들기
- [ ] Android 앱 추가 (패키지명 `com.galaxypods.companion`)
- [ ] `google-services.json` 다운로드 → `app/google-services.json`에 배치
- [ ] `app/build.gradle.kts`에서 다음 주석 제거.
  ```kotlin
  alias(libs.plugins.google.services)
  alias(libs.plugins.firebase.crashlytics)
  // ...
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.crashlytics)
  implementation(libs.firebase.analytics)
  ```
- [ ] `data/system/CrashReporter.kt`의 TODO 부분에서 실제 SDK 호출 활성화
- [ ] 개인정보처리방침 §4 "Crashlytics" 항목을 "도입 검토 중" → "사용 중 (옵트인)"으로 갱신

---

## 4. 빌드 검증

```bash
# JVM 단위 테스트 (88개 케이스)
./gradlew test

# 정적 분석 (PR 게이트와 동일)
./gradlew detekt ktlintCheck :app:lintDebug

# 디버그 APK
./gradlew assembleDebug

# 릴리스 AAB (Play Console 업로드용)
./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

- [ ] 모든 단위 테스트 그린
- [ ] detekt + ktlint + lint 무경고
- [ ] 릴리스 AAB 빌드 성공 + 크기 < 10MB

---

## 5. GitHub Pages 활성화

- [ ] GitHub 리포 Settings → Pages → Source: **GitHub Actions**
- [ ] `main` 브랜치 push → `.github/workflows/pages.yml` 자동 실행
- [ ] 약 1분 후 `https://<username>.github.io/galaxypods/` 접속 확인
- [ ] 4개 페이지 모두 접근 가능 확인.
  - `/privacy-ko/`, `/privacy-en/`
  - `/terms-ko/`, `/terms-en/`
  - `/data-deletion-ko/`, `/data-deletion-en/`
  - `/`

---

## 6. 실기기 검증 매트릭스

검토안 §7.1 참조. **출시 전 최소 8개 조합 실측 권장**.

### Galaxy 단말 (보유한 것 위주)
- [ ] Galaxy S21 (One UI 3 또는 6)
- [ ] Galaxy S23/S24/S25 (One UI 6/7/8)
- [ ] Galaxy Z Fold/Flip (Foldable 적응)

### AirPods/Beats 모델
- [ ] AirPods Pro 2 (USB-C) — 가장 흔함
- [ ] AirPods 4 — 차별화 핵심
- [ ] AirPods Pro 3 — 차별화 핵심
- [ ] AirPods Max
- [ ] Beats 시리즈 1개 이상 (Powerbeats Pro 권장)

### 검증 시나리오 (각 조합당)
- [ ] 케이스 열기 → 풀스크린 알림 표시
- [ ] 양쪽 착용 후 한쪽 빼기 → 음악 자동 정지 (Spotify/YouTube)
- [ ] 양쪽 다시 끼기 → 자동 재생
- [ ] 알림바 아이콘에 % 표시 확인
- [ ] 위젯 추가 후 배터리 갱신 확인
- [ ] FGS 24시간 살아있는지 (One UI 절전 정책 통과)
- [ ] 케이스 멀리 두고 50m 이상 이동 → 분실 알림 (옵션 ON 시)
- [ ] 한국어 음성 안내 (옵션 ON 시) — TTS 발음
- [ ] 위치 기록 → 마지막 위치 화면에서 지도 정상 표시

---

## 7. 권한 정당성 영상 촬영

`store-listing/permission-justification-videos.md` 참조.

- [ ] 영상 1. `FOREGROUND_SERVICE_CONNECTED_DEVICE` (60~75초)
- [ ] 영상 2. `ACCESS_FINE_LOCATION` (60~90초)
- [ ] (선택) 영상 3. `POST_NOTIFICATIONS` + Bluetooth (30~45초)
- [ ] YouTube 일부 공개(Unlisted)로 업로드
- [ ] URL 3개를 Play Console "App content" 양식에 등록

---

## 8. 스토어 리스팅 자료

### 스크린샷 (Phone — 16:9 또는 9:16, 1920×1080 권장)
- [ ] 메인 화면 (배터리 표시 중)
- [ ] 케이스 오픈 알림 화면
- [ ] 마지막 위치 화면 (지도 + 마커)
- [ ] 설정 화면 (귀감지/음성/Samsung)
- [ ] 위젯이 있는 홈화면

### 스크린샷 (Tablet 7" + 10")
- [ ] 메인 화면 (Foldable 펼친 상태)

### 그래픽 자산
- [ ] 앱 아이콘 512×512 (디자이너 작업 또는 Fiverr ~$30)
- [ ] 피처 그래픽 1024×500 (Play Store 배너)

---

## 9. Play Console 등록

### App information
- [ ] 앱 이름. `GalaxyPods` (영어), `GalaxyPods — Galaxy를 위한 무선이어폰 도우미` (한국어)
- [ ] 짧은 설명. `store-listing/{ko-KR,en-US}/short-description.txt`
- [ ] 자세한 설명. `store-listing/{ko-KR,en-US}/full-description.txt`
- [ ] 카테고리. **음악 및 오디오** (또는 도구)
- [ ] 콘텐츠 등급. 자체 등급 설문 — "12세 이용가" 또는 "전체이용가"

### App content
- [ ] **개인정보처리방침 URL** = `https://<username>.github.io/galaxypods/privacy-ko/`
- [ ] **광고 포함 여부** = 아니요
- [ ] **타겟 사용자 연령** = 13세 이상
- [ ] **Data Safety Form** = `store-listing/data-safety-form.md` 그대로 입력
- [ ] **권한 정당성 영상 URL** 등록

### 출시
- [ ] **AAB 업로드** (`app/build/outputs/bundle/release/app-release.aab`)
- [ ] Internal Testing 트랙 → 본인 + 5명 1주
- [ ] Closed Testing 트랙 → 한국 100명 2주 (커뮤니티 모집)
- [ ] Open Beta 트랙 → 1000명 2주
- [ ] Production → 단계 배포 1% → 10% → 50% → 100%

---

## 10. 출시 후 모니터링

- [ ] **Crashlytics 크래시율** < 0.5% 유지
- [ ] **Play Console 평점** 모니터링
- [ ] **사용자 리뷰** 답변 (특히 부정 리뷰 24시간 내)
- [ ] **타깃 SDK 36 마이그레이션** (2026-08 강제)

---

## 11. 일정 권장 (1인 개발자 기준)

| 주차 | 작업 |
|---|---|
| W1 | 빌드/Maps API 키/서명 키 준비 + Firebase 활성화 |
| W2 | 실기기 검증 (5개 모델 이상) |
| W3 | 권한 정당성 영상 3편 촬영 + 업로드 |
| W4 | 스크린샷 5장 + 앱 아이콘 디자인 |
| W5 | Play Console 등록 + Internal Testing 시작 |
| W6 | Internal 피드백 + Closed Testing 시작 |
| W7-8 | Closed Testing |
| W9-10 | Open Beta |
| W11+ | Production 단계 배포 |

총 **약 3개월**. 검토안 §4.1 16주 산정과 일치 (구현 8주 + 출시 8주 → 구현이 빨리 끝났으므로 출시만 약 11주).

---

## 12. 긴급 대응

- **Apple 상표 클레임 수신**. 즉시 스토어 리스팅에서 "AirPods" → "무선 이어폰" 교체 검토.
- **Google Play 정책 거부**. 거부 사유 회신 → SYSTEM_ALERT_WINDOW 옵션 OFF 확인 → 재제출.
- **크래시 폭주**. Crashlytics 확인 → 핫픽스 → 단계 배포 일시 중단.

---

## 참고 문서

- 설계안. `AirPods_Galaxy_앱_상세설계안.md.docx`
- 검토안. `~/.claude/plans/c-galaxypods-airpods-galaxy-md-docx-glimmering-coral.md`
- 경쟁 분석. `competitive-analysis.md`
- 핵심 원칙. `CLAUDE.md`
- 진행 추적. `checklist.md`
- 의사결정. `context-notes.md`
- Play 메타데이터. `store-listing/`
- 법무 문서. `docs/`
