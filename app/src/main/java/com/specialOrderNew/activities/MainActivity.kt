package com.specialOrderNew.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.specialOrderNew.R
import com.specialOrderNew.databinding.ActivityMainBinding
import com.specialOrderNew.utils.debugLog

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onDestroy() {
        super.onDestroy()
        "App has been killed".debugLog(javaClass.simpleName)
    }
}