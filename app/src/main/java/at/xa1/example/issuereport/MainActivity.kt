package at.xa1.example.issuereport

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import at.xa1.example.issuereport.mylib.MyUtil

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MyUtil().useSomeJavaTimeStuffInternally()
    }
}