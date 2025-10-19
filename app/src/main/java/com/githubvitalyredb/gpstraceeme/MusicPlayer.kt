package com.githubvitalyredb.gpstraceeme

import android.content.Context
import android.media.MediaPlayer
import com.githubvitalyredb.gpstraceeme.R

object MusicPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false

    fun start(context: Context) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.background_music)
            mediaPlayer?.isLooping = true
            isPrepared = true
        }
        if (isPrepared) mediaPlayer?.start()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
    }
}
