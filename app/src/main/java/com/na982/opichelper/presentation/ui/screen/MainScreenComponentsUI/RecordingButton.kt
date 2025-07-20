package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.util.Log
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat

@Composable
fun RecordingButton(
    context: Context,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPermissionRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRecording by remember { mutableStateOf(false) }
    
    Log.d("RecordingButton", "Rendering with isRecording=$isRecording")
    
    Button(
        onClick = {
            Log.d("RecordingButton", "Button clicked, current isRecording=$isRecording")
            
            if (isRecording) {
                Log.d("RecordingButton", "Stopping recording")
                onStopRecording()
                isRecording = false
            } else {
                Log.d("RecordingButton", "Checking recording permission")
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                
                if (hasPermission) {
                    Log.d("RecordingButton", "Permission granted, starting recording")
                    onStartRecording()
                    isRecording = true
                    Toast.makeText(context, "녹음을 시작합니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("RecordingButton", "Permission denied, requesting permission")
                    onPermissionRequest()
                    Toast.makeText(context, "녹음 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
            
            Log.d("RecordingButton", "State changed to isRecording=$isRecording")
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRecording) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.secondary
        ),
        modifier = modifier
    ) {
        Text(
            text = if (isRecording) "녹음 중지" else "녹음 시작",
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
} 