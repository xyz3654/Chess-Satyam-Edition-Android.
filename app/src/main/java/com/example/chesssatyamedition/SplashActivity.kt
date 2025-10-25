package com.example.chesssatyamedition

import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val videoView = findViewById<VideoView>(R.id.videoView)

        // THIS LINE IS CHANGED to use your new video.
        // Make sure you have a video named "new_intro.mp4" in your res/raw folder.
        val videoPath = "android.resource://" + packageName + "/" + R.raw.new_intro
        val uri = Uri.parse(videoPath)

        videoView.setVideoURI(uri)

        // --- Code for Audio ---
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()

        videoView.setAudioAttributes(audioAttributes)

        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = false
            mediaPlayer.setVolume(1f, 1f)
        }
        // --- End of Audio Code ---

        // This listener fires when the video finishes playing on its own.
        videoView.setOnCompletionListener {
            goToMainActivity()
        }

        // --- NEW: This listener makes the video skippable ---
        videoView.setOnClickListener {
            // If the user taps the screen, go straight to the main menu.
            goToMainActivity()
        }
        // ----------------------------------------------------

        // This listener handles errors if the video can't be played.
        videoView.setOnErrorListener { _, _, _ ->
            goToMainActivity()
            true // Indicates the error was handled.
        }

        // Start playing the video.
        videoView.start()
    }

    private fun goToMainActivity() {
        // This check prevents the app from trying to open the main menu twice
        // (for example, if the user taps right as the video is ending).
        if (!isFinishing) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
