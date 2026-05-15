# BLE 광고 골든바이트 dump 수집 가이드

본 디렉토리는 `AppleContinuityParser`의 회귀 테스트용 실측 광고 dump를 보관한다.
파일 이름 규칙. `{model}_{state}.hex` (예. `airpods_pro_2_usbc_in_case.hex`)

## 수집 방법

### 1. 도구 준비

- Android. **nRF Connect** (Nordic) 또는 **BLE Scanner**
- iOS. **LightBlue** (참고용, 비교 검증)

### 2. 수집 절차

1. AirPods를 케이스에 넣고 케이스를 닫는다.
2. 단말 BLE 스캔 시작.
3. 케이스를 연다 → 페어링 광고 시작 (약 3초간 active broadcast).
4. nRF Connect에서 manufacturer ID `0x004C` (Apple) 데이터를 확인.
5. **Raw bytes**를 16진수 문자열로 복사 (공백/콜론 무관).
6. 본 디렉토리에 `.hex` 파일로 저장.

### 3. 파일 포맷 예시

`airpods_pro_2_usbc_in_case.hex`.
```
07 19 01 24 20 6B 8F 04 00 04 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
```

- 첫 바이트 `0x07` = Type (Proximity Pairing)
- 두 번째 바이트 `0x19` = Length (25)
- 이후 25바이트 = Value (Device Type 2바이트 + 상태 바이트들)

주석은 `#`로 시작하면 테스트 로더가 무시.

## 수집 우선순위 모델

검토안 §7.1 기준 18개 조합 중 핵심 8개 우선.

- [ ] AirPods 2세대
- [ ] AirPods 3세대
- [ ] AirPods Pro 1세대
- [ ] AirPods Pro 2세대 (Lightning)
- [ ] AirPods Pro 2세대 (USB-C)
- [ ] AirPods 4
- [ ] AirPods 4 (ANC)
- [ ] AirPods Pro 3세대 ★
- [ ] AirPods Max (USB-C) ★

★. v1.0 차별화 핵심.

## 상태 변형

각 모델별로 다음 상태 dump 권장.

1. `in_case_closed` — 케이스 닫힘 (광고 없을 가능성)
2. `case_just_opened` — 케이스 막 열림
3. `both_in_ear` — 양쪽 착용
4. `left_only_in_ear` — 왼쪽만
5. `right_only_in_ear` — 오른쪽만
6. `case_charging` — 케이스 충전 중
7. `low_battery` — 배터리 10% 이하
8. `firmware_v{X}` — 펌웨어 버전 명시

## 기여 시 주의

- BD_ADDR(MAC 주소)은 dump에 포함하지 말 것 (개인정보).
- 광고에는 IRK 회전된 무작위 주소만 포함되므로 raw payload 자체는 식별 불가.
- 테스트 진단용 메모는 `.txt` 동명 파일로 별도 작성.

## 자동 검증

`./gradlew test`는 본 디렉토리의 `.hex` 파일을 모두 로드해 파서가 nullable 아닌 결과를 내는지 확인 (`AppleContinuityParserTest`의 `parametrizedFromDumps`).
