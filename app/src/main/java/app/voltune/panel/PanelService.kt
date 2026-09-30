package app.voltune.panel

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlin.math.abs

class PanelService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var store: ConfigStore
    private lateinit var config: PanelConfig
    private var configHandle: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var trigger: View? = null
    private var panel: PanelView? = null

    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            VolumeWidget.refreshAll(this@PanelService)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        store = ConfigStore(this)
        config = store.load()
        configHandle = store.observe { onConfigChanged() }
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver)
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startInForeground()

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (trigger == null) showTrigger()
        if (intent?.action == ACTION_SHOW) showPanel()
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlays()
        configHandle?.let { store.stopObserving(it) }
        configHandle = null
        contentResolver.unregisterContentObserver(volumeObserver)
        isRunning = false
        super.onDestroy()
    }

    private fun onConfigChanged() {
        config = store.load()
        val panelWasOpen = panel != null
        removeOverlays()
        showTrigger()
        if (panelWasOpen) showPanel()
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.panel_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply { setShowBadge(false) }
        manager.createNotificationChannel(channel)

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val turnOff = PendingIntent.getService(
            this, 1,
            Intent(this, PanelService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(getString(R.string.panel_notification_title))
            .setContentText(getString(R.string.panel_notification_text))
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.action_turn_off), turnOff)
            .setOngoing(true)
            .setSilent(true)
            .build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    @SuppressLint("RtlHardcoded")
    private fun edgeGravity() = if (config.trigger.onLeft) Gravity.LEFT else Gravity.RIGHT

    private fun verticalOffset(): Int {
        val screenHeight = resources.displayMetrics.heightPixels
        return screenHeight * config.trigger.offset / 100
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showTrigger() {
        val settings = config.trigger
        val edge = edgeGravity()

        val pill = View(this).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(settings.thickness).toFloat()
                setColor(settings.color)
                alpha = settings.alpha
            }
        }

        val touchArea = FrameLayout(this).apply {
            addView(
                pill,
                FrameLayout.LayoutParams(
                    dp(settings.thickness),
                    dp(settings.length),
                    edge or Gravity.CENTER_VERTICAL
                )
            )
        }

        var downX = 0f
        var opened = false
        touchArea.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    opened = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val inward = if (settings.onLeft) event.rawX - downX else downX - event.rawX
                    if (!opened && inward > dp(20)) {
                        opened = true
                        showPanel()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!opened && abs(event.rawX - downX) < dp(10)) showPanel()
                }
            }
            true
        }

        val params = overlayParams(
            dp(settings.thickness + 22),
            dp(settings.length + 38)
        ).apply {
            gravity = edge or Gravity.CENTER_VERTICAL
            y = verticalOffset()
        }

        windowManager.addView(touchArea, params)
        trigger = touchArea
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showPanel() {
        if (panel != null) return

        val themed = ContextThemeWrapper(this, R.style.Theme_Voltune)
        val view = PanelView(themed).apply {
            onDndAccessNeeded = {
                post {
                    hidePanel()
                    openDndSettings()
                }
            }
            apply(config)
        }

        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                v.post { hidePanel() }
            }
            false
        }

        val params = overlayParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        ).apply {
            gravity = edgeGravity() or Gravity.CENTER_VERTICAL
            x = dp(20)
        }

        windowManager.addView(view, params)
        panel = view
        trigger?.visibility = View.GONE
    }

    private fun hidePanel() {
        val view = panel ?: return
        windowManager.removeView(view)
        panel = null
        trigger?.visibility = View.VISIBLE
    }

    private fun removeOverlays() {
        hidePanel()
        trigger?.let { windowManager.removeView(it) }
        trigger = null
    }

    private fun openDndSettings() {
        Toast.makeText(this, R.string.dnd_access_hint, Toast.LENGTH_LONG).show()
        startActivity(
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun overlayParams(width: Int, height: Int, extraFlags: Int = 0) =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or extraFlags,
            PixelFormat.TRANSLUCENT
        )

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_STOP = "app.voltune.panel.STOP"
        const val ACTION_SHOW = "app.voltune.panel.SHOW"
        private const val CHANNEL_ID = "floating_panel"
        private const val NOTIFICATION_ID = 1

        var isRunning = false
            private set
    }
}
