package at.xa1.example.issuereport.mylib

import java.time.Duration

class MyUtil {
    fun useSomeJavaTimeStuffInternally(): Boolean {
        return Duration.ofMinutes(-1).isNegative
    }
}