package com.torahanytime.audio.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.torahanytime.audio.ui.home.HomeActivity

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class AudioPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val sessionActivityIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, HomeActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val skipBackCommand = SessionCommand(ACTION_SKIP_BACK, Bundle.EMPTY)
        val skipForwardCommand = SessionCommand(ACTION_SKIP_FORWARD, Bundle.EMPTY)

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(skipBackCommand)
                    .add(skipForwardCommand)
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    ACTION_SKIP_BACK -> {
                        val newPos = (session.player.currentPosition - SKIP_INTERVAL_MS).coerceAtLeast(0)
                        session.player.seekTo(newPos)
                    }
                    ACTION_SKIP_FORWARD -> {
                        val duration = session.player.duration
                        val newPos = session.player.currentPosition + SKIP_INTERVAL_MS
                        session.player.seekTo(if (duration > 0) newPos.coerceAtMost(duration) else newPos)
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .setCallback(callback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_SKIP_BACK = "com.torahanytime.audio.SKIP_BACK"
        const val ACTION_SKIP_FORWARD = "com.torahanytime.audio.SKIP_FORWARD"
        const val SKIP_INTERVAL_MS = 15_000L

        fun buildMediaItem(
            id: String,
            title: String,
            artist: String,
            mp3Url: String,
            thumbnailUrl: String? = null
        ): MediaItem {
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(thumbnailUrl?.let { android.net.Uri.parse(it) })
                .build()

            return MediaItem.Builder()
                .setMediaId(id)
                .setUri(mp3Url)
                .setMediaMetadata(metadata)
                .build()
        }
    }
}
