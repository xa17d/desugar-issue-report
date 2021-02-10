package at.xa1.example.issuereport.mylib

import at.xa1.example.issuereport.externallib.ExternalUtil

class MyUtil {
    fun useSomeJavaTimeStuffInternally(): Boolean {
        return ExternalUtil().useSomeJavaTimeStuffInternally()
    }
}