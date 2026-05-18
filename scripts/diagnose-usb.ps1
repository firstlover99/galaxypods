# GalaxyPods USB 진단 스크립트 — 팅김 원인 파악용
# 사용. PowerShell에서 cd C:\GalaxyPods; .\scripts\diagnose-usb.ps1

# 콘솔 출력 UTF-8 (한글 깨짐 방지)
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$ErrorActionPreference = "Continue"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$pkg = "com.galaxypods.companion.debug"
$apk = "C:\GalaxyPods\app\build\outputs\apk\debug\app-debug.apk"
$logDir = "C:\GalaxyPods\diagnose"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

Write-Host "================================" -ForegroundColor Cyan
Write-Host "  GalaxyPods USB diagnose" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan

# Step 1. ADB devices
Write-Host "`n[1/6] ADB devices..." -ForegroundColor Yellow
$rawOutput = & $adb devices 2>&1
$rawOutput
# 정규식 캡처로 시리얼만 안전하게 추출 (Where-Object + indexing 함정 회피)
$serial = $null
foreach ($l in $rawOutput) {
  $line = "$l"
  if ($line -match "^(\S+)\s+device\s*$") {
    $serial = $matches[1]
    break
  }
}

if (-not $serial) {
  Write-Host "`n[ERROR] No device connected." -ForegroundColor Red
  Write-Host "  1. USB cable supports data transfer"
  Write-Host "  2. Developer options > USB debugging ON"
  Write-Host "  3. Allow USB debugging dialog on phone"
  Write-Host "  4. Samsung USB Driver if needed"
  exit 1
}
Write-Host "  Device serial: $serial" -ForegroundColor Green

# Step 2. Device info
Write-Host "`n[2/6] Device info..." -ForegroundColor Yellow
$brand = (& $adb -s $serial shell getprop ro.product.brand).Trim()
$model = (& $adb -s $serial shell getprop ro.product.model).Trim()
$sdk = (& $adb -s $serial shell getprop ro.build.version.sdk).Trim()
$release = (& $adb -s $serial shell getprop ro.build.version.release).Trim()
$oneUi = (& $adb -s $serial shell getprop ro.build.version.oneui).Trim()
Write-Host "  Brand: $brand"
Write-Host "  Model: $model"
Write-Host "  Android: $release (API $sdk)"
Write-Host "  One UI: $oneUi"

# Step 3. Uninstall
Write-Host "`n[3/6] Uninstall existing..." -ForegroundColor Yellow
& $adb -s $serial uninstall $pkg 2>&1 | Out-Null
Write-Host "  Uninstall done"

# Step 4. Install
Write-Host "`n[4/6] Install new APK..." -ForegroundColor Yellow
if (-not (Test-Path $apk)) {
  Write-Host "[ERROR] APK not found: $apk" -ForegroundColor Red
  exit 1
}
& $adb -s $serial install -r $apk

# Step 5. Clear logcat + launch
Write-Host "`n[5/6] Clear logcat + launch app..." -ForegroundColor Yellow
& $adb -s $serial logcat -c
Start-Sleep -Seconds 1
& $adb -s $serial shell am start -n "$pkg/com.galaxypods.companion.presentation.MainActivity"
Write-Host "  Waiting 6s for crash..."
Start-Sleep -Seconds 6

# Step 6. Extract logs
Write-Host "`n[6/6] Extracting logs..." -ForegroundColor Yellow
$ts = Get-Date -Format "yyyyMMdd-HHmmss"
$logFull = "$logDir\logcat-full-$ts.txt"
$logCrash = "$logDir\logcat-crash-$ts.txt"

& $adb -s $serial logcat -d > $logFull 2>&1
$size = (Get-Item $logFull).Length
Write-Host "  Full log:  $logFull ($([math]::Round($size/1KB, 1)) KB)"

$crashLines = Get-Content $logFull -Encoding UTF8 |
  Select-String -Pattern "GalaxyPods|galaxypods|AndroidRuntime|FATAL|java\.lang|kotlin|dagger|hilt"
$crashLines | Out-File -FilePath $logCrash -Encoding UTF8
$crashCount = ($crashLines | Where-Object { $_ -match "FATAL EXCEPTION" }).Count

Write-Host "  Crash log: $logCrash"
Write-Host "  FATAL EXCEPTION count: $crashCount" -ForegroundColor $(if ($crashCount -gt 0) { "Red" } else { "Green" })

$fatalLines = Get-Content $logFull -Encoding UTF8 | Select-String -Pattern "FATAL EXCEPTION" -Context 0, 30
if ($fatalLines) {
  Write-Host "`n=== FATAL EXCEPTION ===" -ForegroundColor Red
  $fatalLines | Select-Object -First 1
}

Write-Host "`n=== GalaxyPods app logs ===" -ForegroundColor Cyan
$appLines = Get-Content $logFull -Encoding UTF8 | Select-String -Pattern "GalaxyPods/" | Select-Object -First 20
if ($appLines) {
  $appLines
} else {
  Write-Host "  (no GalaxyPods/ tagged logs — App.onCreate did not run)"
}

Write-Host "`n================================" -ForegroundColor Cyan
Write-Host "  Done" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host "Share: $logCrash"
