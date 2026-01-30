// DEPRECATED: Usar versão em android/app/src/main/java/celestemeown/app/android/MainActivity.kt
// Esta pasta (kotlin/) não é compilada como parte do build Android.
// Manter por compatibilidade, mas todas as classes ativas estão em android/app/src/main/java

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
