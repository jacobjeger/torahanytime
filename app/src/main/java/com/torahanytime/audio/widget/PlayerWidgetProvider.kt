package com.torahanytime.audio.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.torahanytime.audio.R
import com.torahanytime.audio.ui.home.HomeActivity

class PlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_PLAY -> {
                // Send a media button broadcast to toggle play/pause
                val mediaIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
                mediaIntent.setPackage(context.packageName)
                context.sendBroadcast(mediaIntent)
                // Also launch the app so the player can handle it
                val launchIntent = Intent(context, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(EXTRA_TOGGLE_PLAY, true)
                }
                context.startActivity(launchIntent)
            }
            ACTION_UPDATE_WIDGET -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "TorahAnytime"
                val speaker = intent.getStringExtra(EXTRA_SPEAKER) ?: "Tap to open"
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)

                val manager = AppWidgetManager.getInstance(context)
                val widgetIds = manager.getAppWidgetIds(
                    ComponentName(context, PlayerWidgetProvider::class.java)
                )
                for (widgetId in widgetIds) {
                    val views = buildRemoteViews(context, title, speaker, isPlaying)
                    manager.updateAppWidget(widgetId, views)
                }
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = buildRemoteViews(context, "TorahAnytime", "Tap to open", false)
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    companion object {
        const val ACTION_TOGGLE_PLAY = "com.torahanytime.audio.TOGGLE_PLAY"
        const val ACTION_UPDATE_WIDGET = "com.torahanytime.audio.UPDATE_WIDGET"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SPEAKER = "extra_speaker"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_TOGGLE_PLAY = "extra_toggle_play"

        /** Call this from PlayerViewModel to update the widget */
        fun updateNowPlaying(context: Context, title: String, speaker: String, isPlaying: Boolean) {
            val intent = Intent(context, PlayerWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_SPEAKER, speaker)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            context.sendBroadcast(intent)
        }

        private fun buildRemoteViews(
            context: Context,
            title: String,
            speaker: String,
            isPlaying: Boolean
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_player)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_speaker, speaker)
            views.setImageViewResource(
                R.id.widget_play_pause,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )

            // Tap title area → open app
            val openIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, HomeActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_title, openIntent)
            views.setOnClickPendingIntent(R.id.widget_speaker, openIntent)

            // Tap play/pause → toggle playback
            val playIntent = PendingIntent.getBroadcast(
                context, 1,
                Intent(context, PlayerWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE_PLAY
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_play_pause, playIntent)

            return views
        }
    }
}
