# Device Owner Provisioning (Kiosk Mode)

1. **Factory reset or use fresh profile.**
2. Enable developer options and USB debugging on the target device.
3. Connect via ADB and run:

   ```bash
   adb shell dpm set-device-owner "com.laurelid/.DeviceAdminReceiver"
   ```

   Replace the component name with your finalized `DeviceAdminReceiver` once implemented. The kiosk flow in this MVP uses lock-task mode and expects the app to be the device owner or whitelisted.

4. Verify lock task packages:

   ```bash
   adb shell dpm set-lock-task-packages com.laurelid com.laurelid
   ```

5. Optionally configure Android Management API or EMM for long-term fleet management. Document enrollment tokens and provisioning QR codes separately.

> TODO: Add finalized device policy manager receiver + provisioning instructions when moving beyond MVP.
