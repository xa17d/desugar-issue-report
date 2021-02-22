package at.xa1.example.issuereport

import androidx.test.ext.junit.runners.AndroidJUnit4
import at.xa1.example.issuereport.mylib.MyUtil
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalDependencyCanUseJavaTime {
    @Test
    fun shouldNotCrashIfExternalDependencyUsesJavaTime() {
        assertTrue(MyUtil().useSomeJavaTimeStuffInternally())
    }
}