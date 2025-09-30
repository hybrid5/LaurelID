package com.laurelid.pos

import com.laurelid.data.VerificationResult
import com.laurelid.util.Logger

class TransactionManager {
    fun record(result: VerificationResult) {
        // TODO: Integrate with local printer/POS systems once hardware is finalized.
        Logger.d(TAG, "Recorded verification result for ${result.subjectDid}: success=${result.success}")
    }

    fun printResult(result: VerificationResult) {
        // TODO: Replace with ESC/POS commands once printer integration is finalized.
        val status = if (result.success) "VERIFIED" else "REJECTED"
        Logger.i(
            TAG,
            "Printer stub – issuer=${result.issuer}, time=${System.currentTimeMillis()}, status=$status"
        )
    }
    
    companion object {
        private const val TAG = "TransactionManager"
    }
}
