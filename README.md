# GalaxyPods

> **⚠️ 프로젝트 보류 (2026-05-19)**
>
> 비루트 Android의 BLE 기술적 한계로 인해 사용자 가치 충분치 않다 판단하여 개발 중단.
> 코드는 그대로 유지. 향후 조건 충족 시 재개 가능 (아래 "재개 조건" 참조).

---

## 한 줄 소개

Galaxy(One UI 3~8) 단말에서 AirPods 사용자가 iOS 수준 기본 경험(배터리 표시·케이스 팝업·귀감지 자동정지·자동연결·마지막 위치)을 무료로 누릴 수 있게 하는 비루트 컴패니언 앱.

---

## 보류 사유 (2026-05-19)

### 핵심 한계 — 비루트 Android의 BLE 기술 천장

| 사용자 기대 | 비루트 Android 실제 | 원인 |
|---|---|---|
| 실시간 배터리 갱신 | **재페어링 시점에만** | iOS 17.5+ 펌웨어가 Type 0x07 송출을 페어링 핸드셰이크 한정으로 변경 |
| 1% 단위 정확도 | **10% 단위만** | BLE 광고가 4비트 nibble (0~15 → 0~100, 10% step)로 인코딩 |
| 한쪽 pod 자동 정지 | **불가능** | Apple ear sensor 신호는 AAP/L2CAP (root 필수) 전용 |
| 정확한 충전 상태 | **불가능 (false positive)** | 페어링 시 charging nibble bit 1+2 무조건 set 패턴 |
| ANC/투명도 제어 | **불가능** | AAP/L2CAP 전용 |
| 터치/스템 컨트롤 우리 앱 처리 | **불가능** | AVRCP 시스템 처리, 우리 영역 밖 |

### 결정 근거

1. **사용자 검증 (Galaxy Note 20 + AirPods Pro 2 USB-C, iOS 17+ 펌웨어).**
   - 10분 캡처 → Type 0x07 0건 (페어링 외)
   - 케이스 열기/닫기 트리거 모두 무반응
   - "재페어링 외엔 배터리 갱신 방법 없음" 확정
2. **OSS 비교 (PodsLink, AndroPods, MaterialPods, CAPod, OpenPods, LibrePods 등).**
   - 비루트 모두 같은 한계
   - LibrePods만 root + Xposed로 해결 (Play Store 불가)
3. **사용자 가치 판단.**
   - "10% 정확도 + 재페어링 시에만 갱신"으로 일상 사용 가치 미흡

---

## 재개 조건 (향후 가능)

다음 중 하나 충족 시 프로젝트 재개 검토 가능.

1. **Apple 펌웨어 정책 변경** — Type 0x07 broadcast 주기적 송출 복원
2. **Android Bluetooth 스택 변경** — `BluetoothDevice.createL2capChannel`에서 PSM 0x1001 (BR/EDR) 허용
3. **Samsung One UI 자체 AirPods 지원** — Samsung Galaxy Wearable이 AirPods 배터리 메타데이터 채움
4. **OEM 협업** — OnePlus/OPPO ColorOS 16처럼 시스템 스택 패치 협업
5. **노선 전환** — root 의무 분기 별도 빌드 (F-Droid 배포)

---

## 보유 자산 (코드 베이스 가치)

### 견고한 기술 구현 (재사용 가능)
- ✅ Apple Continuity BLE Type 0x07 파서 (CAPod 정렬, flip detection, 28개 모델)
- ✅ Bluetooth Classic A2DP/HSP 모니터 (실시간 연결 상태)
- ✅ ScreenOnReceiver (One UI 절전 회복)
- ✅ Persistent Snapshot + 정직한 "X분 전" 표시
- ✅ FGS connectedDevice + 알림 미디어 컨트롤
- ✅ A2DP 끊김 기반 자동 일시정지/재생
- ✅ 한국어 네이티브 UI + TTS 음성 안내
- ✅ Google Maps "마지막 위치" 화면

### Galaxy 특화
- ✅ One UI 버전 감지 + 절전 정책 자동 안내
- ✅ Samsung Quirks 분리 격리

### CI/CD
- ✅ GitHub Actions (detekt + ktlint + lint + test)
- ✅ 28개 단위 테스트 (모두 그린)
- ✅ CAPod 실측 골든 벡터 + 자체 캡처 골든

### 문서
- ✅ CLAUDE.md (프로젝트 원칙)
- ✅ checklist.md (단계별 진행)
- ✅ context-notes.md (의사결정 누적)
- ✅ docs/v2-aap-research.md (v2.0 연구 결과)
- ✅ docs/competitive-analysis.md (경쟁 앱 분석)

---

## 기술 스택

- Kotlin 2.x + Jetpack Compose + Material 3
- Hilt DI
- Coroutines + Flow
- DataStore Preferences
- Android Bluetooth Framework 직접 (외부 라이브러리 없음)
- Gradle 8.x + Version Catalog
- JUnit5 + MockK + Compose UI Test
- GitHub Actions CI

---

## 빌드 / 테스트

```bash
./gradlew assembleDebug          # 디버그 APK
./gradlew test                   # JVM 단위 테스트
./gradlew detekt ktlintCheck     # 정적 분석
```

---

## 참고 자료

- [v2.0 AAP/L2CAP 연구 보고서](docs/v2-aap-research.md)
- [경쟁 앱 분석](docs/competitive-analysis.md)
- [CAPod (참고)](https://github.com/d4rken-org/capod)
- [LibrePods (root 솔루션 참고)](https://github.com/kavishdevar/librepods)
- [furiousMAC continuity (BLE 사양)](https://github.com/furiousMAC/continuity)
- [PoPETs 2020 — Celosia & Cunche](https://petsymposium.org/2020/files/papers/issue1/popets-2020-0003.pdf)

---

## 라이선스

비결정 (보류 상태). 재개 시 정식 라이선스 부여 예정.
참조한 OSS는 모두 GPLv3 (LibrePods/CAPod/OpenPods) — 직접 복사 X, 알고리즘 학습 후 독자 구현.

---

## 보류 직전 마지막 상태 (2026-05-19)

- 33개 commit
- 약 6,000 LoC (Kotlin)
- 28 unit test 모두 그린
- 디버그 APK 정상 빌드/설치/실행
- 실기기 검증 단말. Galaxy S24 Ultra (One UI 8.0) + Galaxy Note 20 Ultra (One UI 5.0)
- 검증 이어폰. AirPods Pro 2 USB-C (iOS 17+ 펌웨어)
