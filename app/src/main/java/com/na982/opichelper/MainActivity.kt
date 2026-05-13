package com.na982.opichelper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat

import com.na982.opichelper.presentation.ui.navigation.AppNavigation
import com.na982.opichelper.presentation.viewmodel.PlaybackViewModel
import com.na982.opichelper.presentation.viewmodel.QaBrowserViewModel
import com.na982.opichelper.ui.theme.OPicHelperThemeWithMemorizeLevel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.na982.opichelper.domain.manager.WakeLockManager
import javax.inject.Inject
import android.util.Log

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var wakeLockManager: WakeLockManager

    private var isFinishing = false
    private var playbackViewModel: PlaybackViewModel? = null
    private var qaViewModel: QaBrowserViewModel? = null
    private var navController: NavHostController? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wakeLockManager.acquireWakeLock()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            val navController = rememberNavController()
            val pvm: PlaybackViewModel = hiltViewModel()
            val qaVm: QaBrowserViewModel = hiltViewModel()
            this@MainActivity.playbackViewModel = pvm
            this@MainActivity.qaViewModel = qaVm
            this@MainActivity.navController = navController

            val isDarkTheme = isSystemInDarkTheme()

            OPicHelperThemeWithMemorizeLevel(
                darkTheme = isDarkTheme,
                memorizeLevel = ""
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(navController = navController)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        playbackViewModel?.onBackgroundMove()
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing) {
            isFinishing = false
        }
        if (!wakeLockManager.isWakeLockHeld()) {
            wakeLockManager.acquireWakeLock()
        }
        playbackViewModel?.onForegroundReturn()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        isFinishing = true
        cleanupAllResources()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        navController?.let { controller ->
            try {
                if (controller.previousBackStackEntry != null) {
                    super.onBackPressed()
                } else {
                    playbackViewModel?.cleanupAllTtsSync()
                    cleanupAllResources()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "백버튼 처리 중 오류", e)
                super.onBackPressed()
            }
        } ?: run {
            super.onBackPressed()
        }
    }

    private fun cleanupAllResources() {
        try {
            lifecycleScope.launch {
                qaViewModel?.cleanupOnAppExit()
            }
            wakeLockManager.releaseWakeLock()
        } catch (e: Exception) {
            Log.e("MainActivity", "리소스 정리 중 오류 발생", e)
        }
    }
}
