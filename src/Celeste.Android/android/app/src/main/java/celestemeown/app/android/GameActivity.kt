package celestemeown.app.android

import android.app.Activity
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController

/**
 * GameActivity: Hospeda MonoGame/C# Game.
 * Iniciada apenas quando usuário clica "Iniciar Jogo" na UI Flutter.
 */
class GameActivity : Activity() {

    private lateinit var contentRootPath: String
    private lateinit var logsRootPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obter paths da Intent
        contentRootPath = intent.getStringExtra("contentRootPath") ?: "${getExternalFilesDir(null)}/Celeste/Content"
        logsRootPath = intent.getStringExtra("logsRootPath") ?: "${getExternalFilesDir(null)}/Celeste/Logs"

        // Aplicar fullscreen
        applyFullscreenConfig()

        // TODO (ETAPA 5): Integrar MonoGame GameActivity
        // Inicializar jogo com paths
        android.util.Log.i("GameActivity", "Started with ContentRoot: $contentRootPath")
    }

    private fun applyFullscreenConfig() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    override fun onResume() {
        super.onResume()
        applyFullscreenConfig()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyFullscreenConfig()
        }
    }
}
