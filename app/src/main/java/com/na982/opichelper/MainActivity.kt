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
import com.na982.opichelper.presentation.ui.screen.MainScreen
import com.na982.opichelper.presentation.viewmodel.MainViewModel
import com.na982.opichelper.ui.theme.OPicHelperTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log

class MainActivity : ComponentActivity() {
    
    private var isFinishing = false // 앱이 실제로 종료되는지 추적
    private var viewModel: MainViewModel? = null // ViewModel 참조 저장
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 권한이 승인됨
        } else {
            // 권한이 거부됨
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 오디오 녹음 권한 확인 및 요청
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 권한이 이미 있음
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                // 권한 설명이 필요한 경우
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                // 권한 요청
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        
        setContent {
            OPicHelperTheme {
                val vm: MainViewModel = viewModel()
                viewModel = vm // ViewModel 참조 저장
                MainScreen(viewModel = vm)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause() - 앱이 백그라운드로 이동")
        // 백그라운드로 이동 시에는 TTS와 하이라이트 유지
        viewModel?.onBackgroundMove()
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume() - 앱이 포그라운드로 복귀")
        // 포그라운드로 복귀 시 상태 확인
        viewModel?.onForegroundReturn()
    }

    override fun onStop() {
        super.onStop()
        Log.d("MainActivity", "onStop() - 앱이 완전히 숨겨짐")
        // onStop에서는 아직 정리하지 않음 (백그라운드에서 복귀 가능)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy() - 앱이 완전히 종료됨")
        isFinishing = true
        // 앱이 완전히 종료될 때만 모든 리소스 정리
        cleanupAllResources()
    }

    override fun onBackPressed() {
        Log.d("MainActivity", "onBackPressed() - 백버튼 눌림")
        // 백버튼으로 앱 종료 시 모든 TTS와 하이라이트 정리
        cleanupAllResources()
        super.onBackPressed()
    }

    /**
     * 모든 리소스를 정리하는 함수
     * - TTS 재생 중지
     * - 하이라이트 초기화
     * - 오디오 플레이어 중지
     */
    private fun cleanupAllResources() {
        Log.d("MainActivity", "모든 리소스 정리 시작")
        
        try {
            // ViewModel의 정리 함수 호출
            viewModel?.cleanupOnAppExit()
            
            // TTS 서비스 중지 (서비스가 실행 중인 경우)
            val ttsServiceIntent = android.content.Intent(this, com.na982.opichelper.presentation.ui.component.TtsService::class.java)
            stopService(ttsServiceIntent)
            
            Log.d("MainActivity", "모든 리소스 정리 완료")
        } catch (e: Exception) {
            Log.e("MainActivity", "리소스 정리 중 오류 발생", e)
        }
    }
}