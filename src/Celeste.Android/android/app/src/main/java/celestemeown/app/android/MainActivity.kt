package celestemeown.app.android

import celestemeown.app.R
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.content.Intent
import android.content.IntentFilter
import android.content.Context
import android.app.DownloadManager
import android.util.Log
import celestemeown.app.android.LogSystem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar

/**
 * MainActivity: Launcher activity (temporarily AppCompatActivity for compilation).
 * TODO: Convert back to FlutterActivity once Flutter Add-to-App is configured.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val CHANNEL = "celestemeown.app/channel"
    }

    private lateinit var assetInstaller: AssetInstaller
    private lateinit var safExporter: SAFExporter
    private var currentDownloadId: Long = -1L
    private var downloadObserver: DownloadObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            // Inicializar LogSystem
            LogSystem.init(this)

            // Inicializar AssetInstaller e SAFExporter
            assetInstaller = AssetInstaller(this)
            safExporter = SAFExporter(this)
            safExporter.initialize(this)

            // Aplicar fullscreen + landscape
            applyFullscreenConfig()

            // Set native layout
            setContentView(R.layout.activity_main)

            // Wire UI
            val statusText = findViewById<TextView>(R.id.status_text)
            val installBtn = findViewById<Button>(R.id.btn_install)
            val extractBtn = findViewById<Button>(R.id.btn_extract)
            val startBtn = findViewById<Button>(R.id.btn_start)
            val exportLogsBtn = findViewById<Button>(R.id.btn_export_logs)
            val progressBar = findViewById<ProgressBar>(R.id.progress)

            fun updateStatus() {
                statusText.text = when (assetInstaller.getStatus()) {
                    AssetInstaller.AssetStatus.INSTALLED -> "Assets: Instalados"
                    AssetInstaller.AssetStatus.READY_TO_EXTRACT -> "Assets: Pronto para extrair"
                    AssetInstaller.AssetStatus.DOWNLOADING -> "Assets: Baixando..."
                    AssetInstaller.AssetStatus.EXTRACTING -> "Assets: Extraindo..."
                    else -> "Assets: Não instalados"
                }
            }

            updateStatus()

            installBtn.setOnClickListener {
                progressBar.visibility = View.VISIBLE
                val success = installAssets()
                progressBar.visibility = View.GONE
                updateStatus()
                Snackbar.make(it, if (success) "Download iniciado" else "Falha ao iniciar download", Snackbar.LENGTH_SHORT).show()
            }

            extractBtn.setOnClickListener {
                progressBar.visibility = View.VISIBLE
                val ok = extractAssets()
                progressBar.visibility = View.GONE
                updateStatus()
                Snackbar.make(it, if (ok) "Extração completa" else "Erro na extração", Snackbar.LENGTH_SHORT).show()
            }

            startBtn.setOnClickListener {
                if (!assetInstaller.areAssetsInstalled()) {
                    Snackbar.make(it, "Instale os assets antes de iniciar", Snackbar.LENGTH_SHORT).show()
                } else {
                    startGame()
                }
            }

            exportLogsBtn.setOnClickListener {
                exportLogs()
                Snackbar.make(it, "Exportando logs...", Snackbar.LENGTH_SHORT).show()
            }
    }

    // Flutter engine integration disabled for now (no Flutter module configured).
    // MethodChannel requires Flutter to be configured. Keep helper methods accessible for future integration.

    private fun applyFullscreenConfig() {
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
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

    /**
     * MethodChannel: getStatus() - Retorna status dos assets
     */
    fun getAssetStatus(): String {
        return when (assetInstaller.getStatus()) {
            AssetInstaller.AssetStatus.NOT_INSTALLED -> "not_installed"
            AssetInstaller.AssetStatus.DOWNLOADING -> "downloading"
            AssetInstaller.AssetStatus.READY_TO_EXTRACT -> "ready_to_extract"
            AssetInstaller.AssetStatus.EXTRACTING -> "extracting"
            AssetInstaller.AssetStatus.INSTALLED -> "installed"
            AssetInstaller.AssetStatus.ERROR -> "error"
        }
    }

    /**
     * MethodChannel: installAssets() - Inicia download de Content.zip
     */
    fun installAssets(): Boolean {
        return try {
            // Registrar observador de download
            downloadObserver = DownloadObserver(this) { downloadId, success ->
                if (success && downloadId == currentDownloadId) {
                    Log.i("MainActivity", "Download concluído, iniciando extração...")
                    extractAssets()
                } else {
                    Log.e("MainActivity", "Download falhou")
                }
            }

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            registerReceiver(downloadObserver, filter, Context.RECEIVER_EXPORTED)

            // Iniciar download
            currentDownloadId = assetInstaller.downloadAssets()
            currentDownloadId != -1L
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao iniciar instalação: ${e.message}")
            false
        }
    }

    /**
     * MethodChannel: extractAssets() - Extrai Content.zip
     */
    fun extractAssets(): Boolean {
        return try {
            assetInstaller.extractAssets()
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao extrair assets: ${e.message}")
            false
        }
    }

    /**
     * MethodChannel: startGame() - Inicia GameActivity
     */
    fun startGame() {
        try {
            if (!assetInstaller.areAssetsInstalled()) {
                Log.e("MainActivity", "Assets não instalados")
                return
            }

            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("contentRootPath", assetInstaller.getContentRootPath())
            intent.putExtra("logsRootPath", getLogsRootPath())
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao iniciar jogo: ${e.message}")
        }
    }

    /**
     * MethodChannel: setFpsEnabled() - Ativar/desativar FPS counter
     */
    fun setFpsEnabled(enabled: Boolean) {
        val prefs = getSharedPreferences("celeste_preferences", MODE_PRIVATE)
        prefs.edit().putBoolean("fps_enabled", enabled).apply()
        Log.i("MainActivity", "FPS counter: ${if (enabled) "ATIVADO" else "DESATIVADO"}")
    }

    /**
     * MethodChannel: setVerboseLogs() - Ativar/desativar logs verbosos
     */
    fun setVerboseLogs(enabled: Boolean) {
        val prefs = getSharedPreferences("celeste_preferences", MODE_PRIVATE)
        prefs.edit().putBoolean("verbose_logs", enabled).apply()
        Log.i("MainActivity", "Verbose logs: ${if (enabled) "ATIVADO" else "DESATIVADO"}")
    }

    /**
     * MethodChannel: getLogs() - Retorna conteúdo dos logs
     */
    fun getLogs(): String {
        return try {
            val logsDir = File(getLogsRootPath())
            if (!logsDir.exists()) return "Sem logs disponíveis"

            val files = logsDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
            files.take(5).joinToString("\n---\n") { file ->
                "${file.name}:\n${file.readText().take(500)}"
            }
        } catch (e: Exception) {
            "Erro ao ler logs: ${e.message}"
        }
    }

    private fun getLogsRootPath(): String {
        return "${getExternalFilesDir(null)}/Celeste/Logs"
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            downloadObserver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao desregistrar observer: ${e.message}")
        }
    }

    // Native UI used; Flutter engine integration removed for now.

    /**
     * SAF: exportLogs() - Exporta logs via Storage Access Framework
     */
    private fun exportLogs() {
        try {
            val logsDir = File(getLogsRootPath())
            if (!logsDir.exists()) {
                Log.e("MainActivity", "Pasta de logs não encontrada")
                return
            }

            val zipFile = File(getExternalFilesDir(null), "celeste_logs_${System.currentTimeMillis()}.zip")
            if (zipFile.exists()) zipFile.delete()

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                val files = logsDir.listFiles() ?: emptyArray()
                for (f in files) {
                    val entry = ZipEntry(f.name)
                    zos.putNextEntry(entry)
                    FileInputStream(f).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }

            safExporter.requestExport(zipFile.name, zipFile)
            Log.i("MainActivity", "Logs empacotados em: ${zipFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao exportar logs: ${e.message}")
        }
    }

    private fun exportScreenshot(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                safExporter.requestExport(file.name, file)
                Log.i("MainActivity", "Screenshot exportado: ${file.absolutePath}")
            } else {
                Log.e("MainActivity", "Screenshot não encontrado: $filePath")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao exportar screenshot: ${e.message}")
        }
    }

}

    