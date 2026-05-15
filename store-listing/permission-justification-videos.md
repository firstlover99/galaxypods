<!-- Google Play Restricted Permissions 정당성 영상 — 촬영 가이드 -->
# 권한 정당성 영상 — 촬영 가이드

Google Play Console에서 다음 권한을 사용하면 정당성 입증 영상 제출이 권장(또는 의무)된다.

1. **`FOREGROUND_SERVICE_CONNECTED_DEVICE`** — Android 14+ 백그라운드 동작
2. **`ACCESS_FINE_LOCATION`** — 정밀 위치
3. **`POST_NOTIFICATIONS`** — 알림 (선택, 권한 정당성 영상 일반 권장)

영상은 YouTube에 **"일부 공개(Unlisted)"**로 업로드 후 Play Console에 URL 등록.

## 공통 촬영 사양

| 항목 | 권장값 |
|---|---|
| 길이 | 30~90초 |
| 해상도 | 1080p (16:9) |
| 자막 | 영어 (Play Console 정책 준수). 한국어 자막은 보조 |
| 음성 | 무음 + 자막 또는 영어 내레이션 |
| 화면 녹화 | Galaxy 실기기 (S23/S24 권장) — 권한 다이얼로그가 OEM 별로 다름 |
| 끝 3초 | "Not affiliated with Apple Inc. AirPods is a trademark of Apple Inc." 자막 |

녹화 도구 권장. AZ Screen Recorder (Galaxy 호환), 또는 ADB `screenrecord`.

```bash
adb shell screenrecord --bit-rate 8000000 /sdcard/video.mp4
adb pull /sdcard/video.mp4
```

---

## 영상 1. `FOREGROUND_SERVICE_CONNECTED_DEVICE` (60~75초)

### 목적

이 권한은 사용자가 앱을 닫아도 무선 이어폰의 상태(배터리, 착용)를 추적하기 위한 것임을 입증한다.

### 스토리보드

| 초 | 화면 | 자막 (영어) | 자막 (한국어 보조) |
|---|---|---|---|
| 0~3 | GalaxyPods 로고 + 부제 | "GalaxyPods — Pods Companion for Galaxy" | GalaxyPods |
| 3~8 | 메인 화면. 양쪽 이어폰 + 케이스 배터리 표시 | "GalaxyPods shows your earbud battery in real time." | 실시간 배터리 표시 |
| 8~15 | 홈 버튼 → 앱이 백그라운드로 → 알림바 풀다운 → FGS 알림 표시 | "When you leave the app, a foreground service keeps the connection alive." | 앱 종료 후에도 연결 유지 |
| 15~25 | 알림바에 배터리 % 변경 (시간 경과 컷) | "The notification updates as battery drains." | 알림이 배터리 변화 반영 |
| 25~40 | 케이스 닫기 → 다시 열기 → 케이스 오픈 알림 표시 | "Without the foreground service, case-open detection wouldn't work." | FGS 없으면 케이스 알림 X |
| 40~50 | 설정 → "백그라운드 동작 끄기" 토글 → 알림 사라짐 | "Users can fully disable background tracking in settings." | 사용자가 백그라운드 끌 수 있음 |
| 50~60 | "Permission used solely for connected-device tracking. No location, no analytics." 텍스트 | (자막) | (자막) |
| 60~75 | 면책 자막 + 로고 페이드아웃 | "Not affiliated with Apple Inc. AirPods is a trademark of Apple Inc." | Apple과 무관함 |

### 핵심 메시지 (Google 심사관이 확인)

1. ✅ FGS는 **연결된 BLE 기기 추적 목적만**으로 사용됨
2. ✅ FGS는 **항상 알림으로 보임** (음성/네트워크/위치에 사용되지 않음)
3. ✅ 사용자가 **언제든 끌 수 있음**
4. ✅ Android 14+ `connectedDevice` 타입 적합

---

## 영상 2. `ACCESS_FINE_LOCATION` (60~90초)

### 목적

위치 권한은 "마지막 위치 보기" 기능을 위해 **선택적**으로 사용되며, 백그라운드 위치 추적이 아님을 입증한다.

### 스토리보드

| 초 | 화면 | 자막 (영어) | 자막 (한국어 보조) |
|---|---|---|---|
| 0~3 | GalaxyPods 로고 | "GalaxyPods — Location Permission Explained" | 위치 권한 설명 |
| 3~10 | 설정 화면 → "마지막 위치 기록" 토글 (기본 OFF) | "Location is OFF by default." | 기본 비활성 |
| 10~20 | 토글 ON → 권한 다이얼로그 → "Allow only while using" 선택 | "Permission is requested only when the user opts in." | 사용자가 켤 때만 요청 |
| 20~35 | 메인 화면 → 이어폰 케이스에 넣고 멀어짐 → 이어폰 끊김 → "마지막 위치 기록됨" 알림 | "Location is recorded ONCE — at the moment the connection drops." | 연결 끊김 시 1회만 기록 |
| 35~50 | "마지막 위치 보기" 화면 → 지도에 한 점 표시 | "The location is stored locally on your device." | 단말 내에만 저장 |
| 50~65 | 설정 → "마지막 위치 삭제" 버튼 → 즉시 삭제 | "User can delete it anytime, or disable the feature entirely." | 언제든 삭제·비활성 가능 |
| 65~75 | "No background tracking. No location upload. No analytics." 텍스트 강조 | (자막) | 백그라운드 추적 없음 |
| 75~90 | 면책 자막 | "Not affiliated with Apple Inc. AirPods is a trademark of Apple Inc." | Apple과 무관 |

### 핵심 메시지

1. ✅ 위치는 **기본 비활성**, 사용자 명시 옵트인 시에만 활성
2. ✅ 위치는 **연결 끊김 시점에 1회만** 수집 (지속 추적 X)
3. ✅ `ACCESS_BACKGROUND_LOCATION` 권한 **사용하지 않음**
4. ✅ 단말 내에만 저장, 외부 전송 없음
5. ✅ 사용자가 즉시 삭제 가능

---

## 영상 3. `POST_NOTIFICATIONS` + Bluetooth 권한 (선택, 30~45초)

### 목적

Android 13+ 알림 권한과 BLE 권한이 핵심 기능에 필수임을 입증.

### 스토리보드

| 초 | 화면 | 자막 (영어) | 자막 (한국어) |
|---|---|---|---|
| 0~3 | 로고 | "GalaxyPods — Bluetooth & Notifications" | Bluetooth와 알림 |
| 3~10 | 첫 실행 → 권한 다이얼로그 (Bluetooth + 알림) | "GalaxyPods asks for Bluetooth and Notification access on first launch." | 첫 실행 시 권한 요청 |
| 10~20 | "Bluetooth → Why" 도움말 → "BLUETOOTH_SCAN with neverForLocation flag" 강조 | "Bluetooth scan with the 'neverForLocation' flag. We do not infer your location from Bluetooth." | 위치 추론 안 함 |
| 20~30 | 케이스를 여는 모습 → 알림 표시 → "이 알림이 없으면 핵심 기능 동작 안 함" | "Notifications power case-open alerts and battery warnings." | 알림이 핵심 기능 구동 |
| 30~45 | 면책 자막 | "Not affiliated with Apple Inc." | Apple과 무관 |

---

## 업로드 가이드 (YouTube)

1. YouTube Studio → 동영상 업로드
2. 가시성. **"일부 공개(Unlisted)"**
3. 제목 예시. `GalaxyPods - Foreground Service Permission Justification`
4. 설명. "Permission justification video for Google Play submission. App: GalaxyPods. Permission: FOREGROUND_SERVICE_CONNECTED_DEVICE."
5. 카테고리. "How-to & Style" 또는 "Science & Technology"
6. 댓글. 사용 안 함
7. 자막 파일(.srt) 업로드 (영어 + 한국어)

업로드 완료 후 URL을 Play Console "App content → Sensitive permissions" 또는 "Restricted permissions" 양식에 붙여넣기.

---

## 촬영 시 주의사항

- ❌ 영상에 다른 앱/계정/연락처가 등장하지 않도록 단말 초기화 후 촬영
- ❌ 위치 권한 다이얼로그가 "Allow all the time"로 표시되면 안 됨 (Android 11+ 변경됨)
- ❌ 자막에 "Apple official", "AirPods official" 같은 표현 절대 금지
- ✅ 끝 3초 면책 자막은 모든 영상에 동일하게
- ✅ 영상 길이 90초 초과 금지 (Google 심사관이 보지 않을 가능성 ↑)
- ✅ 무음으로 촬영 후 후작업으로 자막만 (저작권 음악 회피)

---

## 자가 검증 체크리스트

영상 업로드 전 확인.

- [ ] 영상 길이 30~90초
- [ ] 1080p 16:9
- [ ] 영어 자막 모든 컷에 표시
- [ ] 끝 3초 면책 문구 (Apple Inc. 면책)
- [ ] 권한 사용 화면이 명확하게 보임
- [ ] 사용자 옵트인/옵트아웃 화면이 영상에 등장
- [ ] 외부 서버 통신 없음을 명시
- [ ] YouTube "일부 공개" 설정
- [ ] URL을 Play Console 양식에 붙여넣음

---

## 영상 만들 도구 추천

| 도구 | 용도 | 비용 |
|---|---|---|
| AZ Screen Recorder | 화면 녹화 (Android) | 무료 (Galaxy 호환) |
| OBS Studio | 데스크탑 편집/녹화 | 무료 |
| DaVinci Resolve | 자막·편집 | 무료 |
| CapCut | 모바일 편집·자막 | 무료 |
| Subtitle Edit | .srt 자막 작성 | 무료 |

---

## TODO 사용자 액션

- [ ] AZ Screen Recorder 또는 ADB로 영상 1 촬영
- [ ] 영상 1 자막 작성 + 편집
- [ ] 영상 1 YouTube 일부 공개 업로드
- [ ] 영상 2 동일 절차 반복
- [ ] (선택) 영상 3 동일 절차 반복
- [ ] Play Console 양식에 URL 등록
