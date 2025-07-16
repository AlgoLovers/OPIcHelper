package com.na982.opichelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.na982.opichelper.presentation.ui.screen.MainScreen
import com.na982.opichelper.presentation.viewmodel.MainViewModel
import com.na982.opichelper.ui.theme.OPicHelperTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.na982.opichelper.presentation.ui.component.TtsPlayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect

class MainActivity : ComponentActivity() {
    private lateinit var ttsPlayer: com.na982.opichelper.presentation.ui.component.TtsPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsPlayer = com.na982.opichelper.presentation.ui.component.TtsPlayer(this)
        setContent {
            OPicHelperTheme {
                val viewModel: MainViewModel = viewModel()
                MainScreen(viewModel = viewModel, ttsPlayer = ttsPlayer)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        ttsPlayer.stop()
    }

    override fun onDestroy() {
        ttsPlayer.shutdown()
        super.onDestroy()
    }
}