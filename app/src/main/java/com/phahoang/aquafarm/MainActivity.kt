package com.phahoang.aquafarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import com.phahoang.aquafarm.ui.components.theme.AquaFarmTheme
import com.phahoang.aquafarm.ui.main.AquaFarmApp
import com.phahoang.aquafarm.ui.main.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var vm: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        vm = ViewModelProvider(this)[MainViewModel::class.java]

        // Save data on lifecycle events (pause, stop, destroy)
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> vm.saveNow()
                else -> {}
            }
        })


        setContent {
            AquaFarmTheme {
                AquaFarmApp(vm)
            }
        }
    }
}