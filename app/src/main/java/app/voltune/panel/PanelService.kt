package app.voltune.panel

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.voltune.panel.databinding.OverlayPanelBinding
import kotlin.math.abs

class PanelService : Service() {

    private lateinit var windowManager: WindowManager
    private var trigger: View? = null
    private var panel: OverlayPanelBinding? = null
    private var panelRows: StreamRows? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
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
        hidePanel()
        trigger?.let { windowManager.removeView(it) }
        trigger = null
        isRunning = false
        super.onDestroy()
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

    @SuppressLint("ClickableViewAccessibility")
    private fun showTrigger() {
        val pill = View(this).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(4).toFloat()
                setColor(ContextCompat.getColor(this@PanelService, R.color.vt_primary))
                alpha = 210
            }
        }

        val touchArea = FrameLayout(this).apply {
            addView(pill, FrameLayout.LayoutParams(dp(6), dp(72), Gravity.END or Gravity.CENTER_VERTICAL))
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
                    if (!opened && downX - event.rawX > dp(20)) {
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

        val params = overlayParams(dp(28), dp(110)).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        windowManager.addView(touchArea, params)
        trigger = touchArea
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showPanel() {
        if (panel != null) return

        val themed = ContextThemeWrapper(this, R.style.Theme_Voltune)
        val inflater = LayoutInflater.from(themed)
        val binding = OverlayPanelBinding.inflate(inflater)

        val rows = StreamRows(themed, inflater, binding.panelStreams) {
            binding.root.post {
                hidePanel()
                openDndSettings()
            }
        }
        rows.build()
        rows.startWatching()

        binding.root.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                view.post { hidePanel() }
            }
            false
        }

        val params = overlayParams(
            dp(300),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = dp(20)
        }

        windowManager.addView(binding.root, params)
        panel = binding
        panelRows = rows
        trigger?.visibility = View.GONE
    }

    private fun hidePanel() {
        val binding = panel ?: return
        panelRows?.stopWatching()
        windowManager.removeView(binding.root)
        panel = null
        panelRows = null
        trigger?.visibility = View.VISIBLE
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
