package app.voltune.panel

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.widget.RemoteViews

class VolumeWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refresh(context, manager, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val direction = when (intent.action) {
            ACTION_UP -> AudioManager.ADJUST_RAISE
            ACTION_DOWN -> AudioManager.ADJUST_LOWER
            else -> return
        }

        val audio = context.getSystemService(AudioManager::class.java)
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        refreshAll(context)
    }

    companion object {
        private const val ACTION_UP = "app.voltune.panel.WIDGET_UP"
        private const val ACTION_DOWN = "app.voltune.panel.WIDGET_DOWN"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, VolumeWidget::class.java))
            if (ids.isNotEmpty()) refresh(context, manager, ids)
        }

        private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val audio = context.getSystemService(AudioManager::class.java)
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val percent = audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / max

            val views = RemoteViews(context.packageName, R.layout.widget_panel).apply {
                setTextViewText(
                    R.id.widgetMediaValue,
                    context.getString(R.string.widget_media_format, percent)
                )
                setOnClickPendingIntent(R.id.widgetRoot, openPanel(context))
                setOnClickPendingIntent(R.id.widgetDown, adjust(context, ACTION_DOWN, 1))
                setOnClickPendingIntent(R.id.widgetUp, adjust(context, ACTION_UP, 2))
            }
            manager.updateAppWidget(ids, views)
        }

        private fun openPanel(context: Context) = PendingIntent.getActivity(
            context, 0,
            Intent(context, PanelLauncherActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        private fun adjust(context: Context, action: String, code: Int) = PendingIntent.getBroadcast(
            context, code,
            Intent(context, VolumeWidget::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
