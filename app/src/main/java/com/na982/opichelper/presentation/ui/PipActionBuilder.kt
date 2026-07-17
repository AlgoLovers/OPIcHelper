package com.na982.opichelper.presentation.ui

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.na982.opichelper.MainActivity
import com.na982.opichelper.R
import com.na982.opichelper.domain.audio.PipState

// RemoteAction은 API 26(O)부터 존재한다. PiP 모드 자체가 API 26+ 기능이므로
// 이 빌더는 항상 PiP 진입 이후에만 호출된다(MainActivity.updatePipActions의
// isInPictureInPictureMode 가드). @Suppress로 경고를 숨기는 대신, API 계약을
// 타입에 명시해서 호출부가 잘못 쓰면 컴파일 시점에 걸리도록 한다.
@RequiresApi(Build.VERSION_CODES.O)
class PipActionBuilder(private val context: Context) {

    fun buildActions(state: PipState): List<RemoteAction> {
        val actions = ArrayList<RemoteAction>()

        if (state.hasCompleted) {
            actions.add(buildRepeatAction())
            if (state.hasNextItem) {
                actions.add(buildNextAction())
            }
        } else if (state.isPausable) {
            actions.add(buildPlayPauseAction(state))
            if (state.isRepeatListeningMode && state.isPlaying) {
                actions.add(buildRepeatSentenceAction())
            }
        }

        actions.add(buildStopAction())
        return actions
    }

    private fun buildRepeatAction(): RemoteAction {
        val icon = Icon.createWithResource(context, R.drawable.ic_replay)
        val intent = PendingIntent.getBroadcast(
            context, 2,
            Intent(MainActivity.ACTION_PIP_REPEAT).setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteAction(icon, "반복 재생", "반복 재생", intent)
    }

    private fun buildNextAction(): RemoteAction {
        val icon = Icon.createWithResource(context, R.drawable.ic_skip_next)
        val intent = PendingIntent.getBroadcast(
            context, 3,
            Intent(MainActivity.ACTION_PIP_NEXT).setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteAction(icon, "다음", "다음", intent)
    }

    private fun buildPlayPauseAction(state: PipState): RemoteAction {
        val showPause = state.isPlaying && !state.isPaused
        val icon = Icon.createWithResource(
            context,
            if (showPause) R.drawable.ic_pause else R.drawable.ic_play
        )
        val intent = PendingIntent.getBroadcast(
            context, 0,
            Intent(MainActivity.ACTION_PIP_PLAY_PAUSE).setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        val label = if (showPause) "일시정지" else "재생"
        return RemoteAction(icon, label, label, intent)
    }

    private fun buildRepeatSentenceAction(): RemoteAction {
        val icon = Icon.createWithResource(context, R.drawable.ic_repeat_one)
        val intent = PendingIntent.getBroadcast(
            context, 4,
            Intent(MainActivity.ACTION_PIP_REPEAT_SENTENCE).setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteAction(icon, "문장 반복", "현재 문장 반복", intent)
    }

    private fun buildStopAction(): RemoteAction {
        val icon = Icon.createWithResource(context, R.drawable.ic_stop)
        val intent = PendingIntent.getBroadcast(
            context, 1,
            Intent(MainActivity.ACTION_PIP_STOP).setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteAction(icon, "정지", "정지", intent)
    }
}
