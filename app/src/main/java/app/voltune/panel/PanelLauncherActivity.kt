package app.voltune.panel

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.ContextCompat

class PanelLauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Settings.canDrawOverlays(this)) {
            val show = Intent(this, PanelService::class.java).setAction(PanelService.ACTION_SHOW)
            ContextCompat.startForegroundService(this, show)
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
