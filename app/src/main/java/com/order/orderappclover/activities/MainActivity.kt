package com.order.orderappclover.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.order.orderappclover.R
import com.order.orderappclover.databinding.ActivityMainBinding
import com.order.orderappclover.utils.debugLog

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