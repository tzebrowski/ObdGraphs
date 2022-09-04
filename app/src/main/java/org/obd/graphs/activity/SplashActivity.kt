package org.obd.graphs.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

private const val SPLASH_LOAD_TIME = 300L

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.sleep(SPLASH_LOAD_TIME)
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }
}