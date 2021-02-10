package at.xa1.example.issuereport.externallib

import java.time.Duration

class ExternalUtil {
    fun useSomeJavaTimeStuffInternally(): Boolean {
        return Duration.ofMinutes(-1).isNegative
    }
}