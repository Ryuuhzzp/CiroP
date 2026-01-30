using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;

namespace Celeste.Core.Services
{
    /// <summary>
    /// Gerenciador de Assets para validação no lado C#
    /// </summary>
    public class ContentAssetManager
    {
        private readonly string _contentRootPath;
        private readonly ILogSystem _logSystem;

        public ContentAssetManager(string contentRootPath, ILogSystem logSystem)
        {
            _contentRootPath = contentRootPath ?? throw new ArgumentNullException(nameof(contentRootPath));
            _logSystem = logSystem;
        }

        /// <summary>
        /// Valida se todos os assets críticos estão presentes
        /// </summary>
        public (bool isValid, List<string> missingItems) ValidateAssets()
        {
            var missingItems = new List<string>();

            // Diretórios críticos
            var requiredDirs = new[]
            {
                "Dialogs",
                "Fonts", 
                "Effects",
                "Atlases",
                "Audio",
                "Audio/Banks"
            };

            foreach (var dir in requiredDirs)
            {
                var path = Path.Combine(_contentRootPath, dir);
                if (!Directory.Exists(path))
                {
                    missingItems.Add($"Diretório: {dir}");
                    _logSystem?.LogWarning($"Diretório crítico não encontrado: {path}");
                }
            }

            // Arquivos críticos
            var requiredFiles = new[]
            {
                "Dialogs/english.txt",
                "Fonts/font.fnt",
                "Fonts/font_outline.fnt",
                "Effects/effect_bloom.xnb",
                "Atlases/Gameplay.atlas",
                "Audio/Banks/Master.bank",
                "Audio/Banks/Master.strings.bank"
            };

            foreach (var file in requiredFiles)
            {
                var path = Path.Combine(_contentRootPath, file);
                if (!File.Exists(path))
                {
                    missingItems.Add($"Arquivo: {file}");
                    _logSystem?.LogWarning($"Arquivo crítico não encontrado: {path}");
                }
            }

            bool isValid = missingItems.Count == 0;

            if (isValid)
            {
                _logSystem?.LogInfo("✓ Todos os assets validados com sucesso");
            }
            else
            {
                _logSystem?.LogError($"✗ Faltam {missingItems.Count} assets críticos");
            }

            return (isValid, missingItems);
        }

        /// <summary>
        /// Lista todos os arquivos em um diretório de conteúdo
        /// </summary>
        public List<string> GetContentFiles(string subdir = "")
        {
            try
            {
                var path = string.IsNullOrEmpty(subdir) 
                    ? _contentRootPath 
                    : Path.Combine(_contentRootPath, subdir);

                if (!Directory.Exists(path))
                    return new List<string>();

                var files = new List<string>();
                var info = new DirectoryInfo(path);

                foreach (var file in info.GetFiles("*", SearchOption.AllDirectories))
                {
                    files.Add(file.FullName.Replace(_contentRootPath, "").TrimStart('/'));
                }

                return files;
            }
            catch (Exception ex)
            {
                _logSystem?.LogError($"Erro ao listar arquivos de conteúdo: {ex.Message}");
                return new List<string>();
            }
        }

        /// <summary>
        /// Obter tamanho total do diretório de conteúdo
        /// </summary>
        public long GetContentSizeBytes()
        {
            try
            {
                if (!Directory.Exists(_contentRootPath))
                    return 0;

                var info = new DirectoryInfo(_contentRootPath);
                long totalSize = 0;

                foreach (var file in info.GetFiles("*", SearchOption.AllDirectories))
                {
                    totalSize += file.Length;
                }

                return totalSize;
            }
            catch (Exception ex)
            {
                _logSystem?.LogError($"Erro ao calcular tamanho do conteúdo: {ex.Message}");
                return 0;
            }
        }

        /// <summary>
        /// Obter status do conteúdo em formato legível
        /// </summary>
        public string GetContentStatus()
        {
            var sizeBytes = GetContentSizeBytes();
            var (isValid, missingItems) = ValidateAssets();

            string status = $"Content Root: {_contentRootPath}\n";
            status += $"Status: {(isValid ? "✓ VÁLIDO" : "✗ INVÁLIDO")}\n";
            status += $"Tamanho Total: {FormatBytes(sizeBytes)}\n";

            if (!isValid)
            {
                status += $"Items faltantes: {missingItems.Count}\n";
                foreach (var item in missingItems.Take(5))
                {
                    status += $"  - {item}\n";
                }
                if (missingItems.Count > 5)
                {
                    status += $"  ... e {missingItems.Count - 5} mais\n";
                }
            }

            return status;
        }

        private string FormatBytes(long bytes)
        {
            string[] sizes = { "B", "KB", "MB", "GB" };
            double len = bytes;
            int order = 0;

            while (len >= 1024 && order < sizes.Length - 1)
            {
                order++;
                len = len / 1024;
            }

            return $"{len:0.##} {sizes[order]}";
        }
    }
}
