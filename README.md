# LaurelID Kiosk MVP

## Trust List Endpoint
Update the trust list host by editing `DEFAULT_BASE_URL` inside `app/src/main/java/com/laurelid/network/RetrofitModule.kt` or by entering an override URL in the admin settings screen. The Retrofit client and in-memory cache will automatically use the new endpoint on the next verification attempt or explicit refresh.

## Admin & Configuration Mode
- From the scanner screen, long-press anywhere on the display for five seconds to reveal the PIN prompt.
- Enter the default admin PIN (`123456`) to open the admin console.
- Configure the venue ID, trust list refresh interval (minutes), optional API endpoint override, and toggle demo mode.
- Settings persist via `SharedPreferences` and are applied the next time the scanner resumes.

## Demo Mode
Enabling demo mode from the admin console disables camera/NFC input and alternates between simulated 21+ and under-21 payloads. Each simulated verification is logged so operators know demo data was used.

## Log Retention
Structured JSON verification logs are written to `/data/data/com.laurelid/files/logs/verify.log`. Logs older than 30 days are purged automatically on application startup.
Update the trust list host by editing `BASE_URL` inside `app/src/main/java/com/laurelid/network/RetrofitModule.kt`. The Retrofit client and in-memory cache will automatically use the new endpoint on the next verification attempt or explicit refresh.

## Device Owner Provisioning
See [`DeviceOwnerSetup.md`](DeviceOwnerSetup.md) for step-by-step instructions on making the kiosk the device owner (including `dpm set-device-owner`) so that lock task mode engages automatically on boot.

## Testing the Flow
- **21+ acceptance:** Present a QR code that includes `age21=1` (for example, `mdoc://engagement?age21=1`).
- **Underage/invalid:** Present any QR code without the `age21=1` flag to see the rejection path.
- **NFC:** Tap an mDL credential exposed as an NDEF record with MIME type `application/iso.18013-5+mdoc` to trigger the NFC verification path.

## Known Limitations
- ISO 18013-5 cryptographic verification, revocation checking, and COSE/SD-JWT validation remain TODOs in `ISO18013Parser` and `WalletVerifier`.
- The bundled trust list logic only checks for issuer presence; production deployments must implement full trust chain validation.
- The local Gradle distribution path expects `gradle/local-distributions/gradle-8.14.3-bin.zip`; update or replace the archive if you upgrade Gradle.
