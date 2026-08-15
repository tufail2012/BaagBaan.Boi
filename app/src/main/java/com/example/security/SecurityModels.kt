package com.example.security

enum class UnlockMethod(val title: String, val subtitle: String) {
    BIOMETRIC("Fingerprint", "Unlock using fingerprint or biometric"),
    PIN("PIN", "Unlock with a secure PIN"),
    PATTERN("Pattern", "Unlock using a pattern"),
    PASSWORD("Password", "Unlock with a password")
}

enum class LockAfterDuration(val label: String, val durationMs: Long) {
    IMMEDIATELY("Immediately", 0L),
    SECONDS_15("15 seconds", 15_000L),
    SECONDS_30("30 seconds", 30_000L),
    MINUTE_1("1 minute", 60_000L);

    companion object {
        fun fromDurationMs(ms: Long): LockAfterDuration {
            return entries.firstOrNull { it.durationMs == ms } ?: IMMEDIATELY
        }
    }
}
