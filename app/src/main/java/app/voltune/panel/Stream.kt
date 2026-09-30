package app.voltune.panel

import android.media.AudioManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

enum class Stream(
    val key: String,
    val audioType: Int,
    @StringRes val label: Int,
    @DrawableRes val icon: Int
) {
    MEDIA("media", AudioManager.STREAM_MUSIC, R.string.stream_media, R.drawable.ic_stream_media),
    RING("ring", AudioManager.STREAM_RING, R.string.stream_ring, R.drawable.ic_stream_ring),
    NOTIFICATION(
        "notification",
        AudioManager.STREAM_NOTIFICATION,
        R.string.stream_notification,
        R.drawable.ic_stream_notification
    ),
    ALARM("alarm", AudioManager.STREAM_ALARM, R.string.stream_alarm, R.drawable.ic_stream_alarm),
    CALL("call", AudioManager.STREAM_VOICE_CALL, R.string.stream_call, R.drawable.ic_stream_call),
    SYSTEM("system", AudioManager.STREAM_SYSTEM, R.string.stream_system, R.drawable.ic_stream_system);

    val followsRinger: Boolean
        get() = this == RING || this == NOTIFICATION || this == SYSTEM

    companion object {
        fun fromKey(key: String): Stream? = entries.firstOrNull { it.key == key }
    }
}
