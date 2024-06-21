package com.specialOrder.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.specialOrder.R
import com.specialOrder.fragment.orderHistory.OrderHistoryFragment
import com.specialOrder.utils.exceptionHandler

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onPause() {
        super.onPause()
        exceptionHandler { OrderHistoryFragment.isClicked = true }
    }
}