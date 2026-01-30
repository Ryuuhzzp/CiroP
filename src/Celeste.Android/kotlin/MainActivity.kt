package celestemeown.app.android

import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.content.Intent
import android.content.IntentFilter
import android.content.Context
import android.app.DownloadManager
import android.util.Log
import java.io.File
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/**
 * MainActivity: Launcher activity que hospeda Flutter UI.
 * Também responsável por orquestração do fluxo (instalação de assets, export, etc).
 */
class MainActivity : FlutterActivity() {

    companion object {
        private const val CHANNEL = "celestemeown.app/channel"
    }

    private lateinit var assetInstaller: AssetInstaller
    private lateinit var safExporter: SAFExporter
    private var currentDownloadId: Long = -1L
    private var downloadObserver: DownloadObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar AssetInstaller e SAFExporter
        assetInstaller = AssetInstaller(this)
        safExporter = SAFExporter(this)
        safExporter.initialize(this)

        // Aplicar fullscreen + landscape
        applyFullscreenConfig()

        // MethodChannel será configurado em configureFlutterEngine
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        setupMethodChannel(flutterEngine)
    }

    private fun setupMethodChannel(flutterEngine: FlutterEngine) {
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "getStatus" -> {
                    result.success(getAssetStatus())
                }
                "installAssets" -> {
                    result.success(installAssets())
                }
                "extractAssets" -> {
                    result.success(extractAssets())
                }
                "startGame" -> {
                    startGame()
                    result.success(true)
                }
                "setFpsEnabled" -> {
                    val enabled = call.argument<Boolean>("enabled") ?: false
                    setFpsEnabled(enabled)
                    result.success(true)
                }
                "setVerboseLogs" -> {
                    val enabled = call.argument<Boolean>("enabled") ?: false
                    setVerboseLogs(enabled)
                    result.success(true)
                }
                "getLogs" -> {
                    result.success(getLogs())
                }
                "exportLogs" -> {
                    exportLogs()
                    result.success(true)
                }
                "exportScreenshot" -> {
                    val filePath = call.argument<String>("filePath") ?: ""
                    exportScreenshot(filePath)
                    result.success(true)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        Log.i("MainActivity", "MethodChannel configurado: $CHANNEL")
    }

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

            val zipFile = File(getExternalFilesDir(null), "celeste_logs.zip")
            zipFile.deleteOnExit()
            
            // Criar ZIP dos logs
            val files = logsDir.listFiles() ?: emptyArray()
            if (files.isNotEmpty()) {
                safExporter.requestExport("celeste_logs_${System.currentTimeMillis()}.zip", zipFile)
                Log.i("MainActivity", "Logs exportados via SAF")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao exportar logs: ${e.message}")
        }
    }

    /**
     * SAF: exportScreenshot() - Exporta screenshot via Storage Access Framework
     */
    private fun exportScreenshot(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                safExporter.requestExport("celeste_screenshot_${System.currentTimeMillis()}.png", file)
                Log.i("MainActivity", "Screenshot exportado via SAF")
            } else {
                Log.e("MainActivity", "Screenshot não encontrado: $filePath")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao exportar screenshot: ${e.message}")
        }
    }
}
