package com.unlockedwear.wear.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Trampoline activity for MissionRingComplicationService tap actions.
 */
@AndroidEntryPoint
class MissionRingSelectorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MainWearActivity::class.java).apply {
            putExtra(MainWearActivity.EXTRA_ROUTE, MainWearActivity.ROUTE_RING_SELECTOR)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
}

/**
 * Trampoline activity for CoPilotComplicationService tap actions.
 */
@AndroidEntryPoint
class CoPilotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MainWearActivity::class.java).apply {
            putExtra(MainWearActivity.EXTRA_ROUTE, MainWearActivity.ROUTE_COPILOT)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
}

/**
 * Trampoline activity for ThermalComplicationService tap actions.
 */
@AndroidEntryPoint
class ThermalDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MainWearActivity::class.java).apply {
            putExtra(MainWearActivity.EXTRA_ROUTE, MainWearActivity.ROUTE_THERMAL_DETAIL)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
}
