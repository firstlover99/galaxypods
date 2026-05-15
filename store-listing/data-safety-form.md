<!-- Google Play Console Data Safety 폼 답변 매트릭스 (출시 전 그대로 입력) -->
# Google Play Data Safety Form — 답변 매트릭스

본 문서는 Play Console "App content → Data safety" 폼에 입력할 답변을 사전 정리한 것이다.
실제 폼 입력 시 본 문서를 그대로 옮겨 적는다. 폼 변경 시 본 문서도 동기화.

**마지막 검토.** 2026-05-15 (앱 v0.1.0 기준)

---

## Section 1. Data collection and security

### Q1. Does your app collect or share any of the required user data types?

**답변. Yes**

→ 위치(Location)와 디바이스 식별자(Device or other IDs, BLE 광고 송신 기기의 임시 ID)를 처리하므로 "Yes".

### Q2. Is all of the user data collected by your app encrypted in transit?

**답변. Not collected — Yes**

→ 본 앱은 어떠한 데이터도 외부 서버로 전송하지 않으므로 "in transit" 자체가 발생하지 않음. 폼에는 "Yes"로 응답하되 정당성에 "No data is transmitted; data is processed on-device only" 기재.

### Q3. Do you provide a way for users to request that their data be deleted?

**답변. Yes**

→ "사용자가 앱을 제거하면 모든 단말 내 데이터가 자동 삭제됨. 추가로 앱 내 '설정 → 마지막 위치 삭제'에서 위치 데이터 즉시 삭제 가능."

---

## Section 2. Data types

### 2.1 Location

#### Approximate location

| 항목 | 값 |
|---|---|
| Collected | **No** |
| Shared | **No** |

#### Precise location

| 항목 | 값 |
|---|---|
| Collected | **Yes** |
| Shared | **No** |
| Optional? | **Yes — User can choose whether this data is collected** |
| Purposes | **App functionality** (only) |
| Processed ephemerally? | **No** (저장됨) |
| Required for the app? | **No** |
| Why this data is needed | "Records the last GPS coordinates at the moment the wireless earbud connection drops, so the user can find misplaced earbuds. Collection happens only once per disconnection event, only if the user has explicitly enabled the feature in settings. The data is stored on-device only and is not transmitted." |

### 2.2 Personal info

| 항목 | Collected | Shared |
|---|---|---|
| Name | No | No |
| Email address | No | No |
| User IDs | No | No |
| Address | No | No |
| Phone number | No | No |
| Race and ethnicity | No | No |
| Political or religious beliefs | No | No |
| Sexual orientation | No | No |
| Other info | No | No |

### 2.3 Financial info

→ 모두 No.

### 2.4 Health and fitness

→ 모두 No.

### 2.5 Messages

→ 모두 No.

### 2.6 Photos and videos

→ 모두 No.

### 2.7 Audio files

→ 모두 No.
※ 본 앱은 미디어 키(KEYCODE_MEDIA_PAUSE)만 송신하며 오디오 데이터를 수집/녹음하지 않음.

### 2.8 Files and docs

→ 모두 No.

### 2.9 Calendar

→ 모두 No.

### 2.10 Contacts

→ 모두 No.

### 2.11 App activity

| 항목 | Collected | Shared |
|---|---|---|
| App interactions | No | No |
| In-app search history | No | No |
| Installed apps | No | No |
| Other user-generated content | No | No |
| Other actions | No | No |

### 2.12 Web browsing

→ 모두 No.

### 2.13 App info and performance

| 항목 | Collected | Shared | 비고 |
|---|---|---|---|
| Crash logs | **No** (현재) | No | Crashlytics 도입 시 Yes로 변경 + Optional 지정 |
| Diagnostics | No | No | |
| Other app performance data | No | No | |

**Crashlytics 도입 시 변경 예정 답변.**
- Collected. Yes
- Shared. Yes (with Google Firebase)
- Optional. Yes (사용자 옵트아웃 가능)
- Purpose. App functionality (안정성 개선)
- Why. "Anonymized crash reports are sent to Google Firebase to help diagnose and fix app crashes. Personal identifiers, location, and pairing history are excluded. Users can opt-out in app settings."

### 2.14 Device or other IDs

| 항목 | Collected | Shared |
|---|---|---|
| Device or other IDs | **No** | No |

→ 본 앱은 Android 광고 ID(AAID), 단말 시리얼, IMEI 등 모두 사용하지 않음.
→ BLE 광고에서 수신하는 무선 이어폰의 임시 BD_ADDR(IRK 회전됨)은 식별자가 아닌 일시적 신호로 분류 → "Not collected".

---

## Section 3. Security practices

### Q. Is all of the user data collected by your app encrypted in transit?

**답변. No data is transmitted. (실제 폼은 Yes 또는 No 양자택일이므로 "Yes" 선택 + 설명에 "All data is processed on-device; no transmission occurs.")**

### Q. Do you provide a way for users to request that their data be deleted?

**답변. Yes**

설명. "Users can delete data via Settings → Delete last location, or by uninstalling the app. There is no server-side data."

---

## Section 4. Data deletion request

→ Play Console에서 별도 데이터 삭제 URL 요구 가능. 본 앱은 서버 데이터가 없으므로 다음 URL을 등록.

```
https://firstlover99.github.io/galaxypods/data-deletion-ko/
```

→ 해당 페이지에는 "이 앱은 사용자 데이터를 외부 서버에 저장하지 않습니다. 단말에서 앱을 제거하면 모든 데이터가 즉시 삭제됩니다." 안내 (한/영).

---

## 자가 검증 체크리스트 (제출 전)

- [ ] Privacy Policy URL이 폼과 docs/privacy-en.md 양쪽에서 일치
- [ ] Terms of Service URL이 폼과 docs/terms-en.md 양쪽에서 일치
- [ ] AndroidManifest.xml의 declared permission이 본 폼 답변과 모순 없음 (특히 `BLUETOOTH_SCAN neverForLocation`)
- [ ] 앱이 실제로 인터넷 통신을 시도하지 않음 (`INTERNET` 권한 없음)
- [ ] 위치 권한이 사용자 옵트인 시점에만 요청됨 (Phase 5 구현 시 검증)
- [ ] Crashlytics 도입 시 본 문서 + Privacy Policy 동시 개정

---

## 자주 거부되는 사유 (사전 점검)

1. **"Yes for Precise location, but no clear in-app justification"** — 앱 내에서 위치 권한 요청 직전 다이얼로그에 "분실 방지를 위해 마지막 위치를 1회만 기록합니다" 명시 필수.
2. **"Crash logs collected but Data Safety says No"** — Crashlytics 도입 즉시 본 폼 갱신.
3. **"Privacy Policy URL not accessible"** — GitHub Pages 활성화 후 URL 정상 접근 확인.
4. **"Disclaimer about Apple trademark missing"** — full description, in-app About, privacy policy §9 모두 일관 면책 표시 필요.
