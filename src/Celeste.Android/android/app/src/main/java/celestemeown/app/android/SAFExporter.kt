package com.imshisui.celeste

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class SAFExporter(private val context: Context) {
    private lateinit var createDocumentLauncher: ActivityResultLauncher<String>
    private var fileToExport: File? = null

    fun initialize(activity: AppCompatActivity) {
        createDocumentLauncher = activity.registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream")
        ) { uri ->
            if (uri != null && fileToExport != null) {
                exportFile(uri, fileToExport!!)
            }
        }
    }

    fun requestExport(fileName: String, sourceFile: File) {
        fileToExport = sourceFile
        createDocumentLauncher.launch(fileName)
    }

    private fun exportFile(destinationUri: Uri, sourceFile: File) {
        try {
            val contentResolver = context.contentResolver
            contentResolver.openOutputStream(destinationUri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun getExternalDocumentsDir(context: Context): File? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.getExternalFilesDir(null)
            } else {
                context.getExternalFilesDir(null)
            }
        }
    }
}
