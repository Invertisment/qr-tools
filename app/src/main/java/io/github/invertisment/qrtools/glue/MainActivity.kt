package io.github.invertisment.qrtools.glue

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import io.github.invertisment.qrtools.R

/**
 * Glue: the app's launcher entry point. An input method can't be enabled programmatically —
 * only through system settings — so this screen's only job is a button that jumps there.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<android.view.View>(R.id.open_keyboard_settings_button).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
    }
}
