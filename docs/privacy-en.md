<!-- GalaxyPods Privacy Policy (English version) -->
---
layout: default
title: Privacy Policy
permalink: /privacy-en/
lang: en
---

# GalaxyPods Privacy Policy

**Effective Date.** May 15, 2026
**Last Updated.** May 15, 2026

GalaxyPods (the "App") respects your privacy. This Privacy Policy explains what information the App processes, why, and what rights you have. The App is designed to comply with the Korean Personal Information Protection Act (PIPA), the General Data Protection Regulation (GDPR), and the California Consumer Privacy Act (CCPA).

---

## 1. One-Sentence Summary

**The App does not transmit any personal information to external servers. All data is processed and stored on your device only.**

---

## 2. Information We Process

### 2.1 What We Process

| Item | Purpose | Where | Retention |
|---|---|---|---|
| Bluetooth Low Energy advertising packets from your wireless earbuds (model, battery, in-ear status) | Display on screen, auto play/pause | Device memory | Discarded immediately after display (not persisted) |
| Last known location at the time the earbuds disconnected (optional) | Loss prevention — "Last Location" feature | Device DataStore | Until you delete it manually or uninstall the App |
| App settings (in-ear detection on/off, theme, language) | Preserve your preferences | Device DataStore | Until you uninstall the App |

### 2.2 What We Do **NOT** Process

- Identifying information (name, phone, email, date of birth)
- Payment information, card numbers
- Contacts, call logs, messages
- Audio content played through your earbuds (music, calls)
- Usage records of other apps on your device
- Advertising identifiers (AAID/IDFA)

### 2.3 How We Process

- The App receives BLE advertising signals (publicly broadcast, plaintext) from your earbuds and displays them. The App does not establish a separate secure channel with the earbuds nor access their storage.
- Location data is collected only **once**, at the moment the earbuds disconnect, and only if you have explicitly enabled the "Last Location" feature. The App does not continuously track your location in the background.
- All data is stored locally in Android DataStore, encrypted by the system.

---

## 3. Legal Basis for Processing

| Information | Basis (GDPR Art. 6) |
|---|---|
| BLE advertising packets | Consent (Art. 6(1)(a)) — installation and execution constitute consent |
| Last location coordinates | Explicit consent (Art. 6(1)(a)) — granted at the location permission prompt |
| App settings | Performance of contract (Art. 6(1)(b)) |

---

## 4. Third-Party Sharing and Processors

**The App does not share or transfer your personal information to any third party.**

If we adopt the following service in a future version, this Policy will be amended in advance.

- **Google Firebase Crashlytics** (under consideration) — anonymized crash reports sent to Google LLC. Will not include identifying information, location, or pairing history. Opt-out available in app settings.

---

## 5. Retention Period

- BLE advertising data for display. **Discarded immediately** (not persisted)
- Last location coordinates. **Until you delete or uninstall**
- App settings. **Until you uninstall**

Uninstalling the App automatically deletes all data on your device. There is no server-side data, so no server-side deletion process exists.

---

## 6. Your Rights

You have the right to.

1. **Access** — view stored last-location coordinates in "Settings → View stored data".
2. **Delete** — "Settings → Delete last location" or uninstalling the App.
3. **Object** — disable the location feature in app settings or revoke the location permission in device settings.
4. **Withdraw consent** — uninstalling the App or revoking permissions revokes consent immediately.

Under GDPR Art. 15-22, EU residents additionally have the right to data portability. Since all data is on-device, you can export it via the standard Android backup mechanisms.

Under CCPA, California residents have the right to opt-out of sale of personal information. **The App does not sell any data.**

Exercising these rights incurs no disadvantage.

---

## 7. Permissions and Their Purposes

| Permission | Purpose | If Denied |
|---|---|---|
| `BLUETOOTH_SCAN` (with `neverForLocation` flag) | Receive BLE advertising from earbuds | Core functionality unavailable |
| `BLUETOOTH_CONNECT` | Determine connection state | Connection state not displayed |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Track earbud state after app exits | Background tracking disabled |
| `POST_NOTIFICATIONS` (Android 13+) | Case open notification, low battery alert | Notifications not shown |
| `USE_FULL_SCREEN_INTENT` (Android 14+) | Case open full-screen alert | Case alert not shown |
| `ACCESS_FINE_LOCATION` (optional) | Record last location (when enabled) | Last Location feature unavailable |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Maintain background operation under Samsung power saving | Possible interruption under One UI |
| `RECEIVE_BOOT_COMPLETED` | Auto-restart after device reboot | Manual restart required after reboot |

The `BLUETOOTH_SCAN` permission carries the **`neverForLocation` flag**, which prevents the Android system from inferring location data from BLE scans performed by this app.

---

## 8. Children's Privacy

The App does not knowingly collect personal information from children under 13 (COPPA) or under 14 (Korean PIPA). Users under these ages cannot grant location permission without verifiable parental consent.

---

## 9. Security Measures

- **Encryption.** DataStore uses Android's encrypted local storage.
- **Sandboxing.** The App's data is isolated by Android's app sandbox model.
- **No internet permission.** The App does not declare the `INTERNET` permission (subject to change if Crashlytics is adopted).

---

## 10. Data Controller

- **Controller.** GalaxyPods Operator (real name or business name to be added before release)
- **Contact.** galaxypods.support@gmail.com
- **Inquiry handling.** Email response within 7 business days.

EU residents may also lodge a complaint with their national data protection authority. Korean residents may contact KISA (privacy.go.kr).

---

## 11. International Data Transfers

The App does not transfer data internationally because no data leaves your device. If a future version introduces Crashlytics, crash reports will be transferred to Google LLC (USA) under the EU-US Data Privacy Framework.

---

## 12. Changes to This Policy

If this Policy changes, we will notify you in-app or on this page at least 7 days before the effective date. Material changes adverse to users will be notified at least 30 days in advance.

---

## 13. Effective Date

This Policy is effective as of May 15, 2026.

---

**[한국어 버전](/privacy-ko/)**
