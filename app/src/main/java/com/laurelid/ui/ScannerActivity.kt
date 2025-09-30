package com.laurelid.ui

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import com.laurelid.BuildConfig
import com.laurelid.R
import com.laurelid.auth.ISO18013Parser
import com.laurelid.auth.ParsedMdoc
import com.laurelid.auth.WalletVerifier
import com.laurelid.config.AdminConfig
import com.laurelid.config.ConfigManager
import com.laurelid.data.VerificationResult
import com.laurelid.db.DbModule
import com.laurelid.db.VerificationEntity
import com.laurelid.network.RetrofitModule
import com.laurelid.network.TrustListRepository
import com.laurelid.pos.TransactionManager
import com.laurelid.util.KioskUtil
import com.laurelid.util.LogManager
import com.laurelid.util.Logger
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.text.Charsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.laurelid.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.Charsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var hintText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var debugButton: Button
    private lateinit var configManager: ConfigManager

    private val parser = ISO18013Parser()
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val barcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    private var trustListRepository: TrustListRepository? = null
    private var walletVerifier: WalletVerifier? = null
    private val trustListRepository by lazy {
        TrustListRepository(RetrofitModule.provideTrustListApi(applicationContext))
    }
    private val walletVerifier by lazy { WalletVerifier(trustListRepository) }
    private val verificationDao by lazy { DbModule.provideVerificationDao(applicationContext) }
    private val transactionManager = TransactionManager()

    private var cameraProvider: ProcessCameraProvider? = null
    private var isProcessingCredential = false
    private var currentState: ScannerState = ScannerState.SCANNING
    private var nfcAdapter: NfcAdapter? = null
    private var nfcPendingIntent: PendingIntent? = null
    private var nfcIntentFilters: Array<IntentFilter>? = null
    private var currentConfig: AdminConfig = AdminConfig()
    private var currentBaseUrl: String? = null
    private var demoJob: Job? = null
    private var nextDemoSuccess = true

    private val adminTouchHandler = Handler(Looper.getMainLooper())
    private var adminDialogShowing = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, R.string.error_camera_permission, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        KioskUtil.applyKioskDecor(window)
        KioskUtil.blockBackPress(this)
        configManager = ConfigManager(applicationContext)
        currentConfig = configManager.getConfig()
        rebuildNetworkDependencies(currentConfig)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        bindViews()
        setupAdminGesture()
        if (!currentConfig.demoMode) {
            requestCameraPermission()
            handleNfcIntent(intent)
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        bindViews()
        requestCameraPermission()
        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        currentConfig = configManager.getConfig()
        rebuildNetworkDependencies(currentConfig)
        if (currentConfig.demoMode) {
            stopCamera()
            startDemoMode()
        } else {
            stopDemoMode()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                requestCameraPermission()
            }
        }
        KioskUtil.setImmersiveMode(window)
        if (!currentConfig.demoMode) {
            enableForegroundDispatch()
        } else {
            disableForegroundDispatch()
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
        KioskUtil.setImmersiveMode(window)
        enableForegroundDispatch()
        try {
            startLockTask()
        } catch (throwable: IllegalStateException) {
            Logger.w(TAG, "Unable to enter lock task mode", throwable)
        }
    }

    override fun onPause() {
        super.onPause()
        disableForegroundDispatch()
        stopDemoMode()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!currentConfig.demoMode) {
            handleNfcIntent(intent)
        }
        handleNfcIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        demoJob?.cancel()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }

    override fun onBackPressed() {
        // Block hardware/software back press in kiosk mode.
    }

    private fun bindViews() {
        previewView = findViewById(R.id.previewView)
        statusText = findViewById(R.id.scannerStatus)
        hintText = findViewById(R.id.scannerHint)
        progressBar = findViewById(R.id.scannerProgress)
        debugButton = findViewById(R.id.debugLogButton)
        currentState = ScannerState.RESULT
        updateState(ScannerState.SCANNING)
        if (BuildConfig.DEBUG) {
            debugButton.visibility = View.VISIBLE
            debugButton.setOnClickListener { dumpLatestVerifications() }
        }
    }

    private fun setupAdminGesture() {
        val listener = View.OnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    adminTouchHandler.postDelayed({ promptForAdminPin() }, ADMIN_HOLD_DURATION_MS)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> adminTouchHandler.removeCallbacksAndMessages(null)
            }
            false
        }
        findViewById<View>(R.id.scannerRoot)?.setOnTouchListener(listener)
        previewView.setOnTouchListener(listener)
        findViewById<View>(R.id.overlayContainer)?.setOnTouchListener(listener)
    }

    private fun promptForAdminPin() {
        if (adminDialogShowing) return
        adminDialogShowing = true
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(InputFilter.LengthFilter(6))
            isSingleLine = true
        }
        val container = FrameLayout(this).apply {
            setPadding(48, 32, 48, 0)
            addView(
                input,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.admin_pin_prompt)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val entered = input.text.toString()
                if (entered == ADMIN_PIN) {
                    startActivity(Intent(this, AdminActivity::class.java))
                } else {
                    Toast.makeText(this, R.string.admin_pin_error, Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener {
                adminDialogShowing = false
                adminTouchHandler.removeCallbacksAndMessages(null)
            }
            .show()
    }

    private fun requestCameraPermission() {
        if (currentConfig.demoMode) {
            return
        }
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        if (currentConfig.demoMode || isProcessingCredential) {
        if (isProcessingCredential) {
            return
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
                barcodeScanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val qr = barcodes.firstOrNull { it.rawValue != null && it.format == Barcode.FORMAT_QR_CODE }
                        if (qr?.rawValue != null) {
                            handleQrPayload(qr.rawValue!!)
                        }
                    }
                    .addOnFailureListener { error ->
                        Logger.e(TAG, "Barcode scanning failed", error)
                    }
                    .addOnCompleteListener { imageProxy.close() }
            }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (throwable: Exception) {
                Logger.e(TAG, "Failed to bind camera use cases", throwable)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
    }

    private fun handleQrPayload(payload: String) {
        if (isProcessingCredential) {
            Toast.makeText(this, R.string.toast_processing, Toast.LENGTH_SHORT).show()
            return
        }
        val demo = currentConfig.demoMode
        val effectivePayload = if (demo) nextDemoPayload() else payload
        if (demo) {
            Logger.i(TAG, "Demo mode active; substituting simulated QR payload")
        } else {
            Logger.i(TAG, "QR payload received for verification")
        }
        isProcessingCredential = true
        cameraProvider?.unbindAll()
        updateState(ScannerState.VERIFYING)
        val parsed = parser.parseFromQrPayload(effectivePayload)
        verifyAndPersist(parsed, demoPayloadUsed = demo)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null || currentConfig.demoMode) return
        val action = intent.action ?: return
        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED) return
        Logger.i(TAG, "QR payload received for verification")
        isProcessingCredential = true
        cameraProvider?.unbindAll()
        updateState(ScannerState.VERIFYING)
        val parsed = parser.parseFromQrPayload(payload)
        verifyAndPersist(parsed)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED) return
        if (intent.type != MDL_MIME_TYPE) return
        if (isProcessingCredential) {
            Toast.makeText(this, R.string.toast_processing, Toast.LENGTH_SHORT).show()
            return
        }
        val messages: Array<NdefMessage>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.mapNotNull { it as? NdefMessage }?.toTypedArray()
        }
        val record = messages?.firstOrNull()?.records?.firstOrNull { candidate ->
            candidate.tnf == NdefRecord.TNF_MIME_MEDIA &&
                String(candidate.type, Charsets.US_ASCII) == MDL_MIME_TYPE
        }
        val payload = record?.payload
        if (payload != null) {
            Logger.i(TAG, "NFC payload received for verification")
            isProcessingCredential = true
            updateState(ScannerState.VERIFYING)
            verifyAndPersist(parser.parseFromNfc(payload), demoPayloadUsed = false)
        val payload = messages?.firstOrNull()?.records?.firstOrNull()?.payload
        if (payload != null) {
            isProcessingCredential = true
            updateState(ScannerState.VERIFYING)
            verifyAndPersist(parser.parseFromNfc(payload))
        } else {
            Toast.makeText(this, R.string.toast_nfc_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun handleDemoPayload() {
        if (!currentConfig.demoMode || isProcessingCredential) return
        Logger.i(TAG, "Demo mode injecting simulated credential")
        isProcessingCredential = true
        updateState(ScannerState.VERIFYING)
        val parsed = parser.parseFromQrPayload(nextDemoPayload())
        verifyAndPersist(parsed, demoPayloadUsed = true)
    }

    private fun verifyAndPersist(parsed: ParsedMdoc, demoPayloadUsed: Boolean) {
        val configSnapshot = currentConfig
        val refreshMillis = TimeUnit.MINUTES.toMillis(configSnapshot.trustRefreshIntervalMinutes.toLong())
        lifecycleScope.launch {
            val verifier = ensureWalletVerifier(configSnapshot)
            val verification = runCatching { verifier.verify(parsed, refreshMillis) }
    private fun verifyAndPersist(parsed: ParsedMdoc) {
        lifecycleScope.launch {
            val verification = runCatching { walletVerifier.verify(parsed) }
                .getOrElse { throwable ->
                    Logger.e(TAG, "Verification failed", throwable)
                    VerificationResult(
                        success = false,
                        ageOver21 = parsed.ageOver21,
                        issuer = parsed.issuer,
                        subjectDid = parsed.subjectDid,
                        docType = parsed.docType,
                        error = throwable.message ?: "Verification failure",
                    )
                }

            persistResult(verification, configSnapshot, demoPayloadUsed)
            transactionManager.record(verification)
            transactionManager.printResult(verification)
                        error = throwable.message ?: "Verification failure"
                    )
                }

            persistResult(verification)
            transactionManager.record(verification)
            navigateToResult(verification)
        }
    }

    private fun ensureWalletVerifier(config: AdminConfig): WalletVerifier {
        rebuildNetworkDependencies(config)
        return walletVerifier!!
    }

    private fun resolveBaseUrl(config: AdminConfig): String {
        return config.apiEndpointOverride.takeIf { it.isNotBlank() } ?: RetrofitModule.DEFAULT_BASE_URL
    }

    private fun updateState(state: ScannerState) {
        if (state == currentState) return
        currentState = state
        val textRes = when (state) {
            ScannerState.SCANNING -> R.string.scanner_status_scanning
            ScannerState.VERIFYING -> R.string.scanner_status_verifying
            ScannerState.RESULT -> R.string.scanner_status_ready
        }
        val hintRes = when (state) {
            ScannerState.SCANNING -> R.string.scanner_hint
            ScannerState.VERIFYING -> R.string.scanner_status_verifying
            ScannerState.RESULT -> R.string.scanner_status_ready
        }
        statusText.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction {
                statusText.setText(textRes)
                statusText.animate().alpha(1f).setDuration(150).start()
            }.start()
        hintText.text = getString(hintRes)
        progressBar.visibility = if (state == ScannerState.VERIFYING) View.VISIBLE else View.INVISIBLE
    }

    private fun navigateToResult(result: VerificationResult) {
        updateState(ScannerState.RESULT)
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_SUCCESS, result.success)
            putExtra(ResultActivity.EXTRA_AGE_OVER_21, result.ageOver21)
            putExtra(ResultActivity.EXTRA_ISSUER, result.issuer)
            putExtra(ResultActivity.EXTRA_ERROR, result.error)
        }
        startActivity(intent)
        finish()
    }

    private fun dumpLatestVerifications() {
        lifecycleScope.launch(Dispatchers.IO) {
            val latest = verificationDao.latest(10)
            latest.forEach { Logger.d(TAG, "DB entry: ${'$'}it") }
            latest.forEach { Logger.d(TAG, "DB entry: $it") }
        }
    }

    private suspend fun persistResult(result: VerificationResult, config: AdminConfig, demoPayloadUsed: Boolean) {
    private suspend fun persistResult(result: VerificationResult) {
        withContext(Dispatchers.IO) {
            val entity = VerificationEntity(
                success = result.success,
                subjectDid = result.subjectDid,
                docType = result.docType,
                ageOver21 = result.ageOver21,
                issuer = result.issuer,
                error = result.error,
                tsMillis = System.currentTimeMillis(),
            )
            verificationDao.insert(entity)
            Logger.i(TAG, "Stored verification result for subject ${result.subjectDid}")
            LogManager.appendVerification(applicationContext, result, config, demoPayloadUsed)
            val latest = verificationDao.latest(10)
            latest.forEach { Logger.d(TAG, "Verification log: $it") }
        }
    }

    private fun startDemoMode() {
        if (demoJob != null) return
        Logger.i(TAG, "Starting demo mode loop")
        updateState(ScannerState.SCANNING)
        isProcessingCredential = false
        demoJob = lifecycleScope.launch {
            while (isActive) {
                delay(2500)
                if (!isActive) break
                if (isProcessingCredential) {
                    continue
                }
                handleDemoPayload()
            }
        }
    }

    private fun stopDemoMode() {
        demoJob?.cancel()
        demoJob = null
        if (currentState != ScannerState.VERIFYING) {
            updateState(ScannerState.SCANNING)
        }
        isProcessingCredential = false
    }

    private fun nextDemoPayload(): String {
        val payload = if (nextDemoSuccess) {
            "mdoc://demo?age21=1&issuer=AZ-MVD&subject=demo-${System.currentTimeMillis()}"
        } else {
            "mdoc://demo?age21=0&issuer=AZ-MVD&subject=demo-${System.currentTimeMillis()}"
        }
        nextDemoSuccess = !nextDemoSuccess
        return payload
                tsMillis = System.currentTimeMillis()
            )
            verificationDao.insert(entity)
            Logger.i(TAG, "Stored verification result for subject ${'$'}{result.subjectDid}")
            val latest = verificationDao.latest(10)
            latest.forEach { Logger.d(TAG, "Verification log: ${'$'}it") }
        }
    }

    private fun enableForegroundDispatch() {
        val adapter = nfcAdapter ?: return
        if (nfcPendingIntent == null) {
            val intent = Intent(this, javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        }
        if (nfcIntentFilters == null) {
            val filter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                try {
                    addDataType(MDL_MIME_TYPE)
                } catch (error: IntentFilter.MalformedMimeTypeException) {
                    Logger.e(TAG, "Failed to register NFC MIME type", error)
                }
            }
            nfcIntentFilters = arrayOf(filter)
        }
        val pendingIntent = nfcPendingIntent ?: return
        val intentFilters = nfcIntentFilters
        try {
            adapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
        } catch (throwable: IllegalStateException) {
            Logger.e(TAG, "Failed to enable NFC foreground dispatch", throwable)
        }
    }

    private fun disableForegroundDispatch() {
        try {
            nfcAdapter?.disableForegroundDispatch(this)
        } catch (throwable: IllegalStateException) {
            Logger.w(TAG, "Failed to disable NFC foreground dispatch", throwable)
        }
    }

    private fun rebuildNetworkDependencies(config: AdminConfig) {
        val targetBaseUrl = resolveBaseUrl(config)
        if (walletVerifier == null || targetBaseUrl != currentBaseUrl) {
            Logger.i(TAG, "Configuring trust list repository for baseUrl=$targetBaseUrl")
            try {
                val api = RetrofitModule.provideTrustListApi(applicationContext, targetBaseUrl)
                trustListRepository = TrustListRepository(api)
                walletVerifier = WalletVerifier(trustListRepository!!)
                currentBaseUrl = targetBaseUrl
            } catch (throwable: IllegalArgumentException) {
                Logger.e(TAG, "Invalid trust list URL provided, falling back to default", throwable)
                val fallbackApi = RetrofitModule.provideTrustListApi(applicationContext)
                trustListRepository = TrustListRepository(fallbackApi)
                walletVerifier = WalletVerifier(trustListRepository!!)
                currentBaseUrl = RetrofitModule.DEFAULT_BASE_URL
            }
            val latest = verificationDao.latest(10)
            latest.forEach { Logger.d(TAG, "Verification log: $it") }
        }
    }

    private enum class ScannerState { SCANNING, VERIFYING, RESULT }

    companion object {
        private const val TAG = "ScannerActivity"
        private const val MDL_MIME_TYPE = "application/iso.18013-5+mdoc"
        private const val ADMIN_PIN = "123456"
        private const val ADMIN_HOLD_DURATION_MS = 5000L
    }
}
