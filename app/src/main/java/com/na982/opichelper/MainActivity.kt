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
import com.na982.opichelper.presentation.ui.screen.MainScreen
import com.na982.opichelper.presentation.viewmodel.MainViewModel
import com.na982.opichelper.ui.theme.OPicHelperTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import com.na982.opichelper.domain.manager.WakeLockManager
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var wakeLockManager: WakeLockManager
    
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
        
        // WakeLock 획득 (앱 실행 중 화면 켜짐 유지)
        wakeLockManager.acquireWakeLock()
        Log.d("MainActivity", "앱 시작 - WakeLock 획득")
        
        // 권한 요청
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        
        setContent {
            OPicHelperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause() - 앱이 백그라운드로 이동")
        // 백그라운드로 이동 시에는 TTS와 하이라이트 유지
        // WakeLock은 유지 (사용자가 다시 돌아올 수 있음)
        viewModel?.onBackgroundMove()
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume() - 앱이 포그라운드로 복귀")
        // 포그라운드로 복귀 시 상태 확인
        // WakeLock이 해제되었을 경우 다시 획득
        if (!wakeLockManager.isWakeLockHeld()) {
            wakeLockManager.acquireWakeLock()
            Log.d("MainActivity", "포그라운드 복귀 - WakeLock 재획득")
        }
        viewModel?.onForegroundReturn()
    }

    override fun onStop() {
        super.onStop()
        Log.d("MainActivity", "onStop() - 앱이 완전히 숨겨짐")
        // onStop에서는 아직 정리하지 않음 (백그라운드에서 복귀 가능)
        // WakeLock은 유지 (사용자가 다시 돌아올 수 있음)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy() - 앱이 완전히 종료됨")
        isFinishing = true
        // 앱이 완전히 종료될 때만 모든 리소스 정리
        cleanupAllResources()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Log.d("MainActivity", "onBackPressed() - 백버튼 눌림")
        
        // 백버튼으로 앱 종료 시 완전한 정리 (동기적으로 처리)
        try {
            Log.d("MainActivity", "백버튼 종료 - 완전한 리소스 정리 시작")
            
            // 1. 모든 TTS 강제 중지 (동기적으로)
            viewModel?.cleanupAllTtsSync()
            
            // 2. 모든 리소스 정리
            cleanupAllResources()
            
            Log.d("MainActivity", "백버튼 종료 - 완전한 리소스 정리 완료")
        } catch (e: Exception) {
            Log.e("MainActivity", "백버튼 종료 처리 중 오류", e)
        }
        
        // 앱 완전 종료
        finish()
    }

    /**
     * 모든 리소스를 정리하는 함수
     * - TTS 재생 중지
     * - 하이라이트 초기화
     * - 오디오 플레이어 중지
     * - WakeLock 해제
     */
    private fun cleanupAllResources() {
        Log.d("MainActivity", "모든 리소스 정리 시작")
        
        try {
            // ViewModel의 정리 함수 호출
            viewModel?.cleanupOnAppExit()
            
            // WakeLock 해제
            wakeLockManager.releaseWakeLock()
            Log.d("MainActivity", "WakeLock 해제 완료")
            
            Log.d("MainActivity", "모든 리소스 정리 완료")
        } catch (e: Exception) {
            Log.e("MainActivity", "리소스 정리 중 오류 발생", e)
        }
    }
}