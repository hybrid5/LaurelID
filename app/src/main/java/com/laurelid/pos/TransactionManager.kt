package com.laurelid.pos

import com.laurelid.data.VerificationResult
import com.laurelid.util.Logger

class TransactionManager {
    fun record(result: VerificationResult) {
        // TODO: Integrate with local printer/POS systems once hardware is finalized.
        Logger.d(TAG, "Recorded verification result for ${result.subjectDid}: success=${result.success}")
    }

    companion object {
        private const val TAG = "TransactionManager"
    }
}
