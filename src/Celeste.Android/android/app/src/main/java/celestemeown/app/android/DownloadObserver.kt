package celestemeown.app.android

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Monitora o status de downloads e dispara callbacks
 */
class DownloadObserver(
    private val context: Context,
    private val onDownloadComplete: (downloadId: Long, success: Boolean) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val downloadManager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

            if (downloadManager != null) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = cursor.getInt(columnIndex)
                    
                    val success = status == DownloadManager.STATUS_SUCCESSFUL
                    
                    if (success) {
                        Log.i("DownloadObserver", "Download $downloadId concluído com sucesso")
                    } else {
                        Log.e("DownloadObserver", "Download $downloadId falhou com status: $status")
                    }
                    
                    onDownloadComplete(downloadId, success)
                }
                cursor.close()
            }
        }
    }
}
