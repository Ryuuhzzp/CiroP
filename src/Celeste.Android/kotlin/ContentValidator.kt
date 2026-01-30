package celestemeown.app.android

import java.io.File
import android.util.Log

/**
 * Validador de Content - verifica integridade dos assets extraídos
 */
class ContentValidator(private val contentRootPath: String) {

    fun validate(): ValidationResult {
        val missingItems = mutableListOf<String>()

        // Diretórios críticos
        val requiredDirs = listOf(
            "Dialogs",
            "Fonts",
            "Effects",
            "Atlases",
            "Audio",
            "Audio/Banks"
        )

        for (dir in requiredDirs) {
            val path = File(contentRootPath, dir)
            if (!path.exists() || !path.isDirectory) {
                missingItems.add("Diretório: $dir")
            }
        }

        // Arquivos críticos
        val requiredFiles = listOf(
            "Dialogs/english.txt",
            "Fonts/font.fnt",
            "Fonts/font_outline.fnt",
            "Effects/effect_bloom.xnb",
            "Atlases/Gameplay.atlas",
            "Audio/Banks/Master.bank",
            "Audio/Banks/Master.strings.bank"
        )

        for (file in requiredFiles) {
            val path = File(contentRootPath, file)
            if (!path.exists() || !path.isFile) {
                missingItems.add("Arquivo: $file")
            }
        }

        val isValid = missingItems.isEmpty()
        
        if (isValid) {
            Log.i("ContentValidator", "✓ Todos os assets validados com sucesso")
        } else {
            Log.w("ContentValidator", "✗ Faltam assets: ${missingItems.size} items")
        }

        return ValidationResult(isValid, missingItems)
    }

    data class ValidationResult(
        val isValid: Boolean,
        val missingItems: List<String>
    )
}
