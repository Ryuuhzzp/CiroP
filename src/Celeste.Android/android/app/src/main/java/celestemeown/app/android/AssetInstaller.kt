package celestemeown.app.android

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import java.io.*
import java.net.URI
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Responsável por gerenciar download e extração de Content.zip
 */
class AssetInstaller(private val context: Context) {
    companion object {
        private const val CONTENT_ZIP_URL = "https://github.com/augustjose034-sketch/CODESPACE-/releases/download/X1/Content.zip"
        private const val CONTENT_ZIP_FILENAME = "Content.zip"
        private const val SHARED_PREFS_NAME = "celeste_assets"
        private const val PREFS_KEY_INSTALLED = "assets_installed"
        private const val PREFS_KEY_VERSION = "assets_version"
        private const val ASSETS_VERSION = "1.0"
        private const val BUFFER_SIZE = 8192
    }

    private val contentRootPath: String = context.getExternalFilesDir(null)?.absolutePath + "/Celeste/Content" ?: ""
    private val tempDir: File = context.cacheDir
    private val sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Verifica se os assets estão instalados e válidos
     */
    fun areAssetsInstalled(): Boolean {
        val installed = sharedPrefs.getBoolean(PREFS_KEY_INSTALLED, false)
        val version = sharedPrefs.getString(PREFS_KEY_VERSION, "") ?: ""
        
        if (!installed || version != ASSETS_VERSION) {
            return false
        }

        // Verificar se diretórios críticos existem
        val criticalDirs = listOf(
            "Dialogs",
            "Fonts",
            "Effects",
            "Atlases",
            "Audio/Banks"
        )

        return criticalDirs.all { File(contentRootPath, it).exists() }
    }

    /**
     * Inicia download de Content.zip
     * @return ID do download ou -1 se falhar
     */
    fun downloadAssets(): Long {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(android.net.Uri.parse(CONTENT_ZIP_URL))
            
            request.setTitle("Celeste Assets")
            request.setDescription("Baixando dados do jogo...")
            request.setDestinationInExternalFilesDir(context, null, CONTENT_ZIP_FILENAME)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            
            return downloadManager.enqueue(request)
        } catch (e: Exception) {
            logError("Erro ao iniciar download: ${e.message}")
            return -1L
        }
    }

    /**
     * Extrai Content.zip com proteção contra Zip Slip
     * @return true se sucesso, false se falhar
     */
    fun extractAssets(): Boolean {
        return try {
            val zipFile = File(context.getExternalFilesDir(null), CONTENT_ZIP_FILENAME)
            
            if (!zipFile.exists()) {
                logError("Content.zip não encontrado em: ${zipFile.absolutePath}")
                return false
            }

            // Criar diretório raiz de conteúdo se não existir
            val contentDir = File(contentRootPath)
            if (!contentDir.exists()) {
                contentDir.mkdirs()
            }

            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                
                while (entry != null) {
                    // Proteção contra Zip Slip
                    val name = entry.name
                    if (name.contains("..") || name.startsWith("/")) {
                        logError("Zip Slip detectado: $name")
                        return false
                    }

                    val outputFile = File(contentDir, name)
                    
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile?.mkdirs()
                        
                        FileOutputStream(outputFile).use { fos ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var bytesRead: Int
                            while (zis.read(buffer).also { bytesRead = it } != -1) {
                                fos.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                    
                    entry = zis.nextEntry
                }
            }

            // Marcar como instalado
            sharedPrefs.edit().apply {
                putBoolean(PREFS_KEY_INSTALLED, true)
                putString(PREFS_KEY_VERSION, ASSETS_VERSION)
                apply()
            }

            // Limpar arquivo zip
            zipFile.delete()

            logInfo("Assets extraídos com sucesso para: $contentRootPath")
            return true
        } catch (e: Exception) {
            logError("Erro ao extrair assets: ${e.message}")
            return false
        }
    }

    /**
     * Obtém status de instalação dos assets
     */
    fun getStatus(): AssetStatus {
        return when {
            areAssetsInstalled() -> AssetStatus.INSTALLED
            hasDownloadedZip() -> AssetStatus.READY_TO_EXTRACT
            else -> AssetStatus.NOT_INSTALLED
        }
    }

    /**
     * Verifica se Content.zip foi baixado
     */
    private fun hasDownloadedZip(): Boolean {
        val zipFile = File(context.getExternalFilesDir(null), CONTENT_ZIP_FILENAME)
        return zipFile.exists() && zipFile.length() > 1000 // Arquivo deve ter > 1KB
    }

    /**
     * Retorna caminho raiz do Content
     */
    fun getContentRootPath(): String = contentRootPath

    /**
     * Logging auxiliar
     */
    private fun logInfo(message: String) {
        android.util.Log.i("AssetInstaller", message)
    }

    private fun logError(message: String) {
        android.util.Log.e("AssetInstaller", message)
    }

    enum class AssetStatus {
        NOT_INSTALLED,
        DOWNLOADING,
        READY_TO_EXTRACT,
        EXTRACTING,
        INSTALLED,
        ERROR
    }
}
