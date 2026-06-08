package com.na982.opichelper.domain.audio

interface PlaybackActionListener {
    fun onRepeatQuestion()
    fun onRepeatAnswer()
    fun onNext()
    fun onRepeatMemorization()
    fun onNextAndRestart()
    fun onStopMemorization()
    fun onRepeatCurrentSentence()
}
