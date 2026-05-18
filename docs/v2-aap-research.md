<!-- v2.0 AAP/L2CAP 연구 보고서 — 2026-05-18 -->
# v2.0 AAP/L2CAP 연구 보고서

**작성일.** 2026-05-18
**배경.** v1.0 실기기 검증에서 AirPods Pro 신펌웨어가 Type 0x07 (Proximity Pairing) 광고를 사실상 송출하지 않음을 확인. BLE-only 방식의 한계가 명확해져 v2.0+ AAP/L2CAP 채널 구현 가능성 연구.

---

## 1. 결론 (Executive Summary)

**비루트 Android 앱에서 AAP/L2CAP 직접 구현은 불가능.**

이는 Android 시스템 자체의 제약이며, LibrePods는 root + Magisk 모듈로 `libbluetooth_jni.so` 바이너리 패치를 통해서만 가능. Play Store 정책상 root 의존 앱은 등록 불가.

---

## 2. AAP 프로토콜 기술 사실

### 2.1 채널
- **L2CAP CoC (Connection-oriented Channel)**
- **PSM 0x1001** (4097 decimal)
- **Bluetooth Classic (BR/EDR)** — BLE LE-only 아님
- 일부 자료에서 PSM UUID `74ec2172-0bad-4d01-8f77-997b2be0722a` 언급

### 2.2 패킷 구조
- 모든 패킷은 4바이트 헤더 `0x04 0x00 0x04 0x00`로 시작
- 핸드셰이크 → 인증 → AAP 명령

### 2.3 배터리 패킷 예시
```
04 00 04 00 04 00 [count] ([component] 01 [level] [status] 01)×count
```

### 2.4 활성화되는 기능
- 실시간 배터리 (1% 단위, 케이스 뚜껑 무관)
- in-ear 감지 (즉시)
- ANC / 투명도 모드 제어
- Conversational Awareness
- 헤드 제스처
- 펌웨어 버전 / 모델 정확 식별

---

## 3. Android 시스템 제약 (결정적 장벽)

### 3.1 createL2capChannel API
- `BluetoothDevice.createL2capChannel(int psm)` — API 29+
- **BLE LE-only 지원** (Bluetooth Classic 안 됨)
- **PSM 범위. 0x0001~0x00FF만 허용** — 0x1001 거부

### 3.2 AAP가 필요로 하는 것
- Bluetooth Classic L2CAP CoC
- PSM 0x1001
- 두 조건 모두 Android 일반 앱 API에서 제공 안 함

### 3.3 LibrePods의 해결책 (Play Store 정책 위반)
- Magisk 모듈 `btl2capfix.zip` 설치
- `libbluetooth_jni.so` 바이너리 패치
- PSM 검증 코드 우회
- → **Root 권한 필수**

### 3.4 일부 단말 (Fairphone 5, Pixel 7a 등)에서는 root + 패치로도 실패 보고

---

## 4. 비루트 대안 후보

### 4.1 ❌ Companion Device Manager API
- Android 12+
- 페어링된 기기와 일부 정보 교환 가능
- AAP 채널 자체에는 접근 못 함
- → 배터리 정보 X

### 4.2 ❌ Bluetooth Profile (A2DP) Battery Service
- Bluetooth 5.1+ 배터리 정보 표준
- 그러나 AirPods는 표준 BLE Battery Service 노출 X (Apple 비공개)
- → 동작 안 함

### 4.3 ⚠️ Bluetooth HID Battery Level
- 일부 헤드폰이 HID로 배터리 broadcast
- AirPods는 HID 미사용
- → 동작 안 함

### 4.4 ⚠️ Samsung Galaxy 시스템 API
- One UI는 자체 Bluetooth Service 확장
- AirPods 배터리 표시를 Samsung 단말 자체에서 일부 처리 (Galaxy Wearable 앱 등)
- 비공식 API hook 가능성, Samsung 협업 필요
- 위험. Samsung 변경 시 작동 안 함

### 4.5 ✅ iOS Continuity 모방 (위험)
- 단말이 iOS device인 척 광고 발송
- AirPods가 응답하면 Continuity 정보 수신
- **Apple TOS 위반 가능 + 법적 회색지대**
- 권장 안 함

---

## 5. v2.0 로드맵 재검토

### 5.1 기존 검토안의 가정
- AAP/L2CAP 채널 구현 가능 → 4개월 작업으로 배터리 실시간 수신
- L2CAP "안정화" 후 v2.0 영역

### 5.2 현실 (이 연구 결과)
- 비루트 환경에서 AAP/L2CAP **불가능**
- LibrePods도 같은 한계 → Play Store에 못 올림
- **v2.0 AAP 우선순위 격상 결정 (commit 19cdf02)은 재검토 필요**

### 5.3 새 v2.0 로드맵 후보

#### A. 정직 노선 (권장)
- v1.0 출시 — 케이스 뚜껑 열기 트리거 한정 동작
- 스토어 설명. "구펌웨어 AirPods / AirPods 2/3 / Max 정확 동작. 신펌웨어는 제한적"
- v2.0 — 다른 차별화 기능 우선 (Edge Panel, Bixby Routines, 카카오톡 ducking, Galaxy Watch 모듈)
- AAP는 백로그로만 유지 (Android API 정책 변경 또는 Samsung 협업 시)

#### B. Companion Device Manager 활용
- Android 12+ CompanionDeviceManager로 페어링 정보 활용
- 배터리는 못 받으나 페어링 상태 정확 추적
- in-ear 감지 일부 가능
- 작업 시간 2~3주

#### C. Samsung 협업 (장기)
- Samsung Galaxy Wearable 팀에 비공식 협업 제안
- 단말 시스템 API로 AirPods 배터리 접근
- 채택 가능성 낮음, 시간 매우 길음

---

## 6. 권고 결정

**v2.0 AAP/L2CAP 직접 구현 계획을 백로그로 강등.** 대신.

1. **v1.0 출시 우선** — 케이스 뚜껑 트리거 한정 동작 + 스토어에 명확히 명시
2. **v1.1 — UX 강화 + 다른 기능** — Edge Panel, Bixby Routines, 카카오톡 ducking, Galaxy Watch 모듈
3. **v2.0 — Companion Device Manager 활용 검토** (시간 적당)
4. **AAP/L2CAP는 백로그 유지** — Android API 정책 변경 또는 Samsung 협업 시 부활

---

## 7. 출처 (검증된 1차 자료)

- [LibrePods AAP Definitions.md](https://github.com/kavishdevar/librepods/blob/main/AAP%20Definitions.md) — 1차 자료, 라이선스 GPLv3 (문서만 참조, 코드 복사 X)
- [LibrePods Communication Managers (DeepWiki)](https://deepwiki.com/kavishdevar/librepods/3.3-communication-managers)
- [LibrePods Android Implementation (DeepWiki)](https://deepwiki.com/kavishdevar/librepods/3-android-implementation)
- [capod Issue #215: L2CAP connection to AirPods](https://github.com/d4rken-org/capod/issues/215)
- [LibrePods Issue #229: L2CAP fails on Pixel 7a](https://github.com/kavishdevar/librepods/issues/229)
- [LibrePods Issue #232: L2CAP fails on Fairphone 5](https://github.com/kavishdevar/librepods/issues/232)
- [AAP Protocol Definition (tyalie)](https://github.com/tyalie/AAP-Protocol-Defintion)
- [Microsoft Docs: BluetoothDevice.CreateL2capChannel](https://learn.microsoft.com/en-us/dotnet/api/android.bluetooth.bluetoothdevice.createl2capchannel)
- [Apple devforum: AirPods 4 Bluetooth Firmware Bug in L2CAP](https://origin-devforums.apple.com/forums/thread/816622)
- PoPETs 2020 (Celosia & Cunche, 검토안 §16)
