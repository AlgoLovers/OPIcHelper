package com.na982.opichelper.presentation.viewmodel

interface PlaybackActionListener {
    fun onRepeatQuestion()
    fun onRepeatAnswer()
    fun onNext()
    fun onRepeatMemorization()
    fun onNextAndRestart()
    fun onStopMemorization()
}
