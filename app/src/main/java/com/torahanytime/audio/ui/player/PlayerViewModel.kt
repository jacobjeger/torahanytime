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
import com.torahanytime.audio.TATApplication
import com.torahanytime.audio.data.local.entity.ListeningHistory
import com.torahanytime.audio.data.local.entity.QueueItem
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.player.AudioPlayerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PlayerState(
    val currentLecture: Lecture? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1f,
    val sleepTimerMinutes: Int? = null,
    val queueSize: Int = 0
)

class PlayerViewModel(private val context: Context) : ViewModel() {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private var mediaController: MediaController? = null
    private val db = TATApplication.db
    private var historySaveCounter = 0
    private var sleepTimerJob: Job? = null

    init {
        connectToService()
        // Observe queue size
        viewModelScope.launch {
            db.queueDao().getCount().collect { count ->
                _state.value = _state.value.copy(queueSize = count)
            }
        }
    }

    private fun connectToService() {
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.value = _state.value.copy(isPlaying = isPlaying)
                    // Save position when pausing
                    if (!isPlaying) saveHistory()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        saveHistory()
                        playNextInQueue()
                    }
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
                // Save history every ~10 seconds (20 ticks * 500ms)
                historySaveCounter++
                if (historySaveCounter >= 20 && _state.value.isPlaying) {
                    historySaveCounter = 0
                    saveHistory()
                }
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

    private fun saveHistory() {
        val lecture = _state.value.currentLecture ?: return
        val position = _state.value.currentPosition
        viewModelScope.launch {
            db.historyDao().upsert(
                ListeningHistory(
                    lectureId = lecture.id,
                    title = lecture.title,
                    speakerName = lecture.speakerFullName,
                    thumbnailUrl = lecture.thumbnailUrl,
                    mp3Url = lecture.mp3Url,
                    duration = lecture.duration ?: 0,
                    position = position,
                    languageName = lecture.languageName,
                    lastPlayedAt = System.currentTimeMillis()
                )
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
            // Resume from saved position
            viewModelScope.launch {
                val history = db.historyDao().getByLectureId(lecture.id)
                if (history != null && history.position > 0) {
                    val dur = (lecture.duration ?: 0) * 1000L
                    // Only resume if not at the end (>95% complete)
                    if (dur > 0 && history.position < dur * 0.95) {
                        seekTo(history.position)
                    }
                }
            }
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

    // Queue management
    fun addToQueue(lecture: Lecture) {
        viewModelScope.launch {
            val maxPos = db.queueDao().getMaxPosition() ?: -1
            db.queueDao().insert(
                QueueItem(
                    lectureId = lecture.id,
                    title = lecture.title,
                    speakerName = lecture.speakerFullName,
                    thumbnailUrl = lecture.thumbnailUrl,
                    mp3Url = lecture.mp3Url,
                    duration = lecture.duration ?: 0,
                    languageName = lecture.languageName,
                    position = maxPos + 1
                )
            )
        }
    }

    private fun playNextInQueue() {
        viewModelScope.launch {
            val next = db.queueDao().getNext()
            if (next != null) {
                db.queueDao().removeFirst()
                playLecture(Lecture(
                    id = next.lectureId,
                    title = next.title,
                    speakerNameFirst = next.speakerName.split(" ").firstOrNull(),
                    speakerNameLast = next.speakerName.split(" ").drop(1).joinToString(" "),
                    mp3Url = next.mp3Url,
                    thumbnailUrl = next.thumbnailUrl,
                    duration = next.duration,
                    languageName = next.languageName
                ))
            }
        }
    }

    fun clearQueue() {
        viewModelScope.launch { db.queueDao().deleteAll() }
    }

    // Sleep timer
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _state.value = _state.value.copy(sleepTimerMinutes = minutes)
        sleepTimerJob = viewModelScope.launch {
            delay(minutes * 60 * 1000L)
            mediaController?.pause()
            _state.value = _state.value.copy(sleepTimerMinutes = null)
        }
    }

    fun setSleepTimerEndOfLecture() {
        sleepTimerJob?.cancel()
        _state.value = _state.value.copy(sleepTimerMinutes = -1) // -1 = end of lecture
        sleepTimerJob = viewModelScope.launch {
            // Wait for current lecture to end
            while (_state.value.isPlaying) {
                delay(1000)
            }
            // Don't play next in queue
            _state.value = _state.value.copy(sleepTimerMinutes = null)
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _state.value = _state.value.copy(sleepTimerMinutes = null)
    }

    fun stop() {
        saveHistory()
        mediaController?.stop()
        _state.value = PlayerState()
    }

    override fun onCleared() {
        saveHistory()
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
