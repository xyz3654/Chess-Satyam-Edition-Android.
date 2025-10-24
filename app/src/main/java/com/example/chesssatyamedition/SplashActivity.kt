package com.example.chesssatyamedition

import android.content.Intent
import android.media.AudioAttributes // <-- NEWLY IMPORTED
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val videoView = findViewById<VideoView>(R.id.videoView)

        val videoPath = "android.resource://" + packageName + "/" + R.raw.chessintro
        val uri = Uri.parse(videoPath)

        videoView.setVideoURI(uri)

        // --- START OF NEW CODE FOR AUDIO ---

        // 1. Define audio attributes to play media sound
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()

        videoView.setAudioAttributes(audioAttributes)

        // 2. Set a listener for when the video is ready to play
        videoView.setOnPreparedListener { mediaPlayer ->
            // The video is prepared, now we can enable audio
            mediaPlayer.isLooping = false // Make sure the video doesn't loop
            // By default, the media player might be muted. This line is not always
            // necessary but is good practice to ensure volume is on.
            mediaPlayer.setVolume(1f, 1f)
        }

        // --- END OF NEW CODE FOR AUDIO ---

        // This listener now correctly fires when the video (with audio) is finished
        videoView.setOnCompletionListener {
            goToMainActivity()
        }

        videoView.setOnErrorListener { _, _, _ ->
            goToMainActivity()
            true
        }

        // Start playing the video. The OnPreparedListener will handle the audio.
        videoView.start()
    }

    private fun goToMainActivity() {
        if (!isFinishing) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
