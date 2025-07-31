package com.na982.opichelper.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.na982.opichelper.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthRepositoryImpl(private val context: Context) : AuthRepository {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn
    
    private val _currentUser = MutableStateFlow<GoogleSignInAccount?>(null)
    override val currentUser: StateFlow<GoogleSignInAccount?> = _currentUser
    
    init {
        // 앱 시작 시 저장된 로그인 상태 확인
        val savedLoginState = prefs.getBoolean("is_logged_in", false)
        _isLoggedIn.value = savedLoginState
        
        if (savedLoginState) {
            // 저장된 사용자 정보 복원 (간단한 정보만)
            val userName = prefs.getString("user_name", "게스트 사용자")
            val userEmail = prefs.getString("user_email", null)
            // 실제 GoogleSignInAccount 객체는 복원할 수 없으므로 null로 설정
            _currentUser.value = null
        }
    }
    
    override fun saveLoginState(account: GoogleSignInAccount?) {
        _isLoggedIn.value = true
        _currentUser.value = account
        
        // SharedPreferences에 저장
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_name", account?.displayName ?: "게스트 사용자")
            putString("user_email", account?.email)
            putString("user_id", account?.id)
            apply()
        }
    }
    
    override fun saveGuestLogin() {
        _isLoggedIn.value = true
        _currentUser.value = null
        
        // 게스트 로그인 상태 저장
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_name", "게스트 사용자")
            putString("login_type", "guest")
            apply()
        }
    }
    
    override fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
        
        // SharedPreferences에서 로그인 정보 삭제
        prefs.edit().apply {
            putBoolean("is_logged_in", false)
            remove("user_name")
            remove("user_email")
            remove("user_id")
            remove("login_type")
            apply()
        }
    }
    
    override fun getLoginType(): String {
        return prefs.getString("login_type", "guest") ?: "guest"
    }
    
    override fun getUserName(): String {
        return prefs.getString("user_name", "게스트 사용자") ?: "게스트 사용자"
    }
    
    override fun getUserEmail(): String? {
        return prefs.getString("user_email", null)
    }
} 