package org.obd.graphs.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

private const val SPLASH_LOAD_TIME = 2000L

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        Thread.sleep(SPLASH_LOAD_TIME)
        finish()
    }
}