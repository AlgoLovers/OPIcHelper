package com.na982.opichelper.domain.repository

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val isLoggedIn: StateFlow<Boolean>
    val currentUser: StateFlow<GoogleSignInAccount?>
    
    fun saveLoginState(account: GoogleSignInAccount?)
    fun saveGuestLogin()
    fun logout()
    fun getLoginType(): String
    fun getUserName(): String
    fun getUserEmail(): String?
} 