package io.zwiftboard.android

internal class LeftPodUnlockState {
    enum class Value {
        UNKNOWN,
        LOCKED,
        UNLOCKING,
        UNLOCKED,
    }

    var value = Value.UNKNOWN
        private set
    private var challenged = false

    fun onChallenge(signedIn: Boolean) {
        challenged = true
        value = if (signedIn) Value.UNLOCKING else Value.LOCKED
    }

    fun onSolveResult(success: Boolean) {
        if (challenged) {
            value = if (success) Value.UNLOCKING else Value.LOCKED
        }
    }

    fun onButtonData() {
        if (challenged) value = Value.UNLOCKED
    }

    fun reset() {
        challenged = false
        value = Value.UNKNOWN
    }
}
