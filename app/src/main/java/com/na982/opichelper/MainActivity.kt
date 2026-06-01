package com.na982.opichelper

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.na982.opichelper.domain.manager.WakeLockController
import com.na982.opichelper.presentation.ui.navigation.AppNavigation
import com.na982.opichelper.presentation.viewmodel.PlaybackViewModel
import com.na982.opichelper.presentation.viewmodel.QaBrowserViewModel
import com.na982.opichelper.presentation.viewmodel.RepeatListeningViewModel
import com.na982.opichelper.presentation.viewmodel.EnglishWritingTestViewModel
import com.na982.opichelper.presentation.viewmodel.FullMemorizationViewModel
import com.na982.opichelper.domain.usecase.ModeGroup
import com.na982.opichelper.ui.theme.OPicHelperTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var wakeLockController: WakeLockController

    private var isFinishing = false
    private var playbackViewModel: PlaybackViewModel? = null
    private var qaViewModel: QaBrowserViewModel? = null
    private var navController: NavHostController? = null

    private val _permissionDenied = MutableStateFlow(false)
    val permissionDenied = _permissionDenied.asStateFlow()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            _permissionDenied.value = true
        }
    }

    private val pipActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_PIP_PLAY_PAUSE -> playbackViewModel?.togglePlayPause()
                ACTION_PIP_STOP -> playbackViewModel?.stopPlayback()
                ACTION_PIP_REPEAT -> playbackViewModel?.repeatPlayback()
                ACTION_PIP_NEXT -> playbackViewModel?.playNextItem()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wakeLockController.acquire()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        registerPipActionReceiver()
        setPipParams()

        setContent {
            val navController = rememberNavController()
            val pvm: PlaybackViewModel = hiltViewModel()
            val qaVm: QaBrowserViewModel = hiltViewModel()
            val repeatListeningVm: RepeatListeningViewModel = hiltViewModel()
            val englishWritingTestVm: EnglishWritingTestViewModel = hiltViewModel()
            val fullMemorizationVm: FullMemorizationViewModel = hiltViewModel()
            this@MainActivity.playbackViewModel = pvm
            this@MainActivity.qaViewModel = qaVm
            this@MainActivity.navController = navController

            pvm.setRepeatQuestionCallback {
                val qaItem = qaVm.uiState.value.currentQaItem
                if (qaItem != null) pvm.playQuestion(qaItem.questionEn)
            }
            pvm.setRepeatAnswerCallback {
                val qaItem = qaVm.uiState.value.currentQaItem
                if (qaItem != null) pvm.playAnswer(qaVm.getCurrentAnswer(qaItem))
            }
            pvm.setNextCallback {
                lifecycleScope.launch {
                    val qaItem = qaVm.nextQaItemSync()
                    if (qaItem != null) {
                        pvm.playAnswer(qaVm.getCurrentAnswer(qaItem))
                    }
                }
            }
            pvm.setRepeatMemorizationCallback {
                val group = pvm.lastMemorizationGroup ?: return@setRepeatMemorizationCallback
                when (group) {
                    ModeGroup.REPEAT_LISTENING -> repeatListeningVm.start()
                    ModeGroup.ENGLISH_WRITING -> englishWritingTestVm.start()
                    ModeGroup.FULL_MEMORIZATION -> fullMemorizationVm.start()
                    else -> {}
                }
            }
            pvm.setNextAndRestartCallback {
                lifecycleScope.launch {
                    val qaItem = qaVm.nextQaItemSync()
                    if (qaItem != null) {
                        val group = pvm.lastMemorizationGroup ?: return@launch
                        when (group) {
                            ModeGroup.REPEAT_LISTENING -> repeatListeningVm.start()
                            ModeGroup.ENGLISH_WRITING -> englishWritingTestVm.start()
                            ModeGroup.FULL_MEMORIZATION -> fullMemorizationVm.start()
                            else -> {}
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                qaVm.uiState.collect { state ->
                    pvm.setHasNextItem(qaVm.hasNextQaItem())
                }
            }

            val isDarkTheme = isSystemInDarkTheme()

            OPicHelperTheme(
                darkTheme = isDarkTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        navController = navController,
                        permissionDenied = this@MainActivity.permissionDenied
                    )
                }
            }
        }

        observePipState()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (playbackViewModel?.shouldEnterPip() == true) {
                enterPipMode()
            }
        }
    }

    @Suppress("NewApi")
    private fun setPipParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(5, 3))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                params.setAutoEnterEnabled(false)
            }
            setPictureInPictureParams(params.build())
        }
    }

    @Suppress("NewApi")
    private fun enterPipMode() {
        try {
            val params = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(5, 3))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                params.setAutoEnterEnabled(true)
            }
            val entered = enterPictureInPictureMode(params.build())
            if (!entered) {
                Log.w("MainActivity", "PiP 진입 실패")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "enterPipMode 예외", e)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        playbackViewModel?.setPipMode(isInPictureInPictureMode)
        if (!isInPictureInPictureMode) {
            playbackViewModel?.onForegroundReturn()
        } else {
            playbackViewModel?.onBackgroundMove()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInPictureInPictureMode) {
            if (playbackViewModel?.shouldEnterPip() == true) {
                enterPipMode()
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !isInPictureInPictureMode) {
            playbackViewModel?.onBackgroundMove()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing) {
            isFinishing = false
        }
        if (!wakeLockController.isHeld()) {
            wakeLockController.acquire()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !isInPictureInPictureMode) {
            playbackViewModel?.onForegroundReturn()
        }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        isFinishing = true
        playbackViewModel?.cleanupAllTtsSync()
        unregisterPipActionReceiver()
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
            wakeLockController.release()
        } catch (e: Exception) {
            Log.e("MainActivity", "리소스 정리 중 오류 발생", e)
        }
    }

    @Suppress("NewApi")
    private fun registerPipActionReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val filter = IntentFilter().apply {
                addAction(ACTION_PIP_PLAY_PAUSE)
                addAction(ACTION_PIP_STOP)
                addAction(ACTION_PIP_REPEAT)
                addAction(ACTION_PIP_NEXT)
            }
            registerReceiver(pipActionReceiver, filter, RECEIVER_NOT_EXPORTED)
        }
    }

    private fun unregisterPipActionReceiver() {
        try {
            unregisterReceiver(pipActionReceiver)
        } catch (_: Exception) { }
    }

    @Suppress("NewApi")
    private fun observePipState() {
        lifecycleScope.launch {
            while (playbackViewModel == null) {
                kotlinx.coroutines.delay(50)
            }
            playbackViewModel?.pipState?.collect { state ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    updatePipAutoEnter(state.isPlaying)
                    if (isInPictureInPictureMode) {
                        updatePipActions(state.isPlaying, state.isPaused, state.isPausable, state.hasCompleted, state.hasNextItem)
                    }
                }
            }
        }
    }

    @Suppress("NewApi")
    private fun updatePipAutoEnter(isPlaying: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val params = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(5, 3))
                .setAutoEnterEnabled(isPlaying)
            setPictureInPictureParams(params.build())
        }
    }

    @Suppress("NewApi")
    private fun updatePipActions(isPlaying: Boolean, isPaused: Boolean, isPausable: Boolean, hasCompleted: Boolean, hasNextItem: Boolean) {
        if (!isInPictureInPictureMode) return

        val params = android.app.PictureInPictureParams.Builder()
            .setAspectRatio(android.util.Rational(5, 3))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            params.setAutoEnterEnabled(isPlaying)
        }

        val actions = ArrayList<android.app.RemoteAction>()

        if (hasCompleted) {
            val repeatIcon = Icon.createWithResource(this, R.drawable.ic_replay)
            val repeatIntent = PendingIntent.getBroadcast(
                this, 2,
                Intent(ACTION_PIP_REPEAT),
                PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(
                android.app.RemoteAction(repeatIcon, "반복 재생", "반복 재생", repeatIntent)
            )

            if (hasNextItem) {
                val nextIcon = Icon.createWithResource(this, R.drawable.ic_skip_next)
                val nextIntent = PendingIntent.getBroadcast(
                    this, 3,
                    Intent(ACTION_PIP_NEXT),
                    PendingIntent.FLAG_IMMUTABLE
                )
                actions.add(
                    android.app.RemoteAction(nextIcon, "다음", "다음", nextIntent)
                )
            }
        } else if (isPausable) {
            val showPause = isPlaying && !isPaused
            val playPauseIcon = Icon.createWithResource(
                this,
                if (showPause) R.drawable.ic_pause else R.drawable.ic_play
            )
            val playPauseIntent = PendingIntent.getBroadcast(
                this, 0,
                Intent(ACTION_PIP_PLAY_PAUSE),
                PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(
                android.app.RemoteAction(
                    playPauseIcon,
                    if (showPause) "일시정지" else "재생",
                    if (showPause) "일시정지" else "재생",
                    playPauseIntent
                )
            )
        }

        val stopIcon = Icon.createWithResource(this, R.drawable.ic_stop)
        val stopIntent = PendingIntent.getBroadcast(
            this, 1,
            Intent(ACTION_PIP_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        actions.add(
            android.app.RemoteAction(
                stopIcon, "정지", "정지", stopIntent
            )
        )

        params.setActions(actions)
        setPictureInPictureParams(params.build())
    }

    companion object {
        const val ACTION_PIP_PLAY_PAUSE = "com.na982.opichelper.PIP_PLAY_PAUSE"
        const val ACTION_PIP_STOP = "com.na982.opichelper.PIP_STOP"
        const val ACTION_PIP_REPEAT = "com.na982.opichelper.PIP_REPEAT"
        const val ACTION_PIP_NEXT = "com.na982.opichelper.PIP_NEXT"
    }
}
