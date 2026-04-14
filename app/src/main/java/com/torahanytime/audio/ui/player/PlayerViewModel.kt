package com.torahanytime.audio.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.player.AudioPlayerService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PlayerState(
    val currentLecture: Lecture? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1f
)

class PlayerViewModel(private val context: Context) : ViewModel() {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private var mediaController: MediaController? = null

    init {
        connectToService()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.value = _state.value.copy(isPlaying = isPlaying)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updatePosition()
                }
            })
            startPositionUpdates()
        }, MoreExecutors.directExecutor())
    }

    private fun startPositionUpdates() {
        viewModelScope.launch {
            while (true) {
                updatePosition()
                delay(500)
            }
        }
    }

    private fun updatePosition() {
        mediaController?.let { controller ->
            _state.value = _state.value.copy(
                currentPosition = controller.currentPosition.coerceAtLeast(0),
                duration = controller.duration.coerceAtLeast(0),
                isPlaying = controller.isPlaying
            )
        }
    }

    fun playLecture(lecture: Lecture) {
        val mp3Url = lecture.mp3Url ?: return
        val mediaItem = AudioPlayerService.buildMediaItem(
            id = lecture.id.toString(),
            title = lecture.title,
            artist = lecture.speakerFullName,
            mp3Url = mp3Url,
            thumbnailUrl = lecture.thumbnailUrl
        )
        _state.value = _state.value.copy(currentLecture = lecture)
        mediaController?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
    }

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun skipForward(seconds: Int = 15) {
        mediaController?.let { it.seekTo(it.currentPosition + seconds * 1000L) }
    }

    fun skipBackward(seconds: Int = 15) {
        mediaController?.let { it.seekTo((it.currentPosition - seconds * 1000L).coerceAtLeast(0)) }
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
        _state.value = _state.value.copy(playbackSpeed = speed)
    }

    fun stop() {
        mediaController?.stop()
        _state.value = PlayerState()
    }

    override fun onCleared() {
        mediaController?.release()
        super.onCleared()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(context.applicationContext) as T
        }
    }
}
