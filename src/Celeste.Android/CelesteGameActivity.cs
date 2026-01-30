using System;
using Celeste;
using Monocle;

namespace Celeste.Android;

/// <summary>
/// GameActivity principal do Celeste para Android.
/// Integra PlatformPaths, LogSystem e ExternalContentManager.
/// Será totalmente integrado com MonoGame quando workload Android estiver disponível.
/// </summary>
public class CelesteGameActivity
{
    // Configurações passadas pela Intent/Kotlin
    public string ContentRootPath { get; set; } = string.Empty;
    public bool FpsCounterEnabled { get; set; }
    public bool VerboseLogsEnabled { get; set; }
    public bool TouchOverlayEnabled { get; set; }
    public string LogsRootPath { get; set; } = string.Empty;
    public string SavesRootPath { get; set; } = string.Empty;

    private ExternalFileContentManager? _externalContent;
    private ILogSystem? _logSystem;
    private IPlatformPaths? _platformPaths;

    public CelesteGameActivity()
    {
        // Inicializar será feito via Initialize()
    }

    /// <summary>
    /// Inicializar todos os serviços de plataforma.
    /// Deve ser chamado ANTES de qualquer operação do Celeste.
    /// </summary>
    public void Initialize()
    {
        try
        {
            // 1. Garantir caminhos
            if (string.IsNullOrEmpty(ContentRootPath))
            {
                ContentRootPath = System.IO.Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments), 
                    "Celeste", "Content");
            }
            if (string.IsNullOrEmpty(LogsRootPath))
            {
                LogsRootPath = System.IO.Path.Combine(
                    System.IO.Path.GetDirectoryName(ContentRootPath) ?? ".", "Logs");
            }
            if (string.IsNullOrEmpty(SavesRootPath))
            {
                SavesRootPath = System.IO.Path.Combine(
                    System.IO.Path.GetDirectoryName(ContentRootPath) ?? ".", "Saves");
            }

            // 2. Criar PlatformPaths
            _platformPaths = new AndroidPlatformPaths(ContentRootPath, LogsRootPath, SavesRootPath);
            Monocle.Engine.SetPlatformPaths(_platformPaths);
            LogBootInfo("PlatformPaths registered with Engine");

            // 3. Inicializar FileLogSystem
            _logSystem = new FileLogSystem(LogsRootPath);
            _logSystem.LogInfo($"=== CELESTE GAME ACTIVITY INITIALIZED ===");
            _logSystem.LogInfo($"Content Root: {ContentRootPath}");
            _logSystem.LogInfo($"Logs Root: {LogsRootPath}");
            _logSystem.LogInfo($"Saves Root: {SavesRootPath}");
            _logSystem.LogInfo($"FPS Counter: {FpsCounterEnabled}");
            _logSystem.LogInfo($"Verbose Logs: {VerboseLogsEnabled}");
            _logSystem.LogInfo($"Touch Overlay: {TouchOverlayEnabled}");

            // 4. Inicializar ExternalContentManager
            _externalContent = new ExternalFileContentManager(ContentRootPath);
            _logSystem.LogInfo("ExternalContentManager initialized");

            // 5. Configurar AppDomain para capturar exceptions não tratadas
            AppDomain.CurrentDomain.UnhandledException += (sender, e) =>
            {
                if (_logSystem != null && e.ExceptionObject is Exception ex)
                {
                    _logSystem.CaptureException(ex);
                }
            };

            _logSystem.LogInfo("CelesteGameActivity initialization complete");
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[CelesteGameActivity] ERROR during Initialize: {ex}");
            if (_logSystem != null)
            {
                _logSystem.LogError("Initialize failed", ex);
                _logSystem.FlushLogs();
            }
            throw;
        }
    }

    /// <summary>
    /// Log de boot (antes do FileLogSystem estar disponível).
    /// </summary>
    private void LogBootInfo(string message)
    {
        Console.WriteLine($"[CelesteGameActivity] {message}");
    }

    public void LoadContent()
    {
        try
        {
            _logSystem?.LogInfo("LoadContent called");
            // TODO: Carregar conteúdo de forma segura
            // Usar _externalContent para carregar XNBs do filesystem
        }
        catch (Exception ex)
        {
            _logSystem?.LogError("LoadContent failed", ex);
        }
    }

    public void Update(double deltaTime)
    {
        try
        {
            // Lógica principal do Celeste
        }
        catch (Exception ex)
        {
            _logSystem?.LogError($"Update failed (delta: {deltaTime})", ex);
        }
    }

    public void Draw(double deltaTime)
    {
        try
        {
            // Desenhar o jogo
            if (FpsCounterEnabled)
            {
                DrawFpsCounter(deltaTime);
            }
        }
        catch (Exception ex)
        {
            _logSystem?.LogError($"Draw failed", ex);
        }
    }

    private void DrawFpsCounter(double deltaTime)
    {
        // TODO: Implementar renderização de FPS
        // Usar fonte do jogo se disponível
        // Por enquanto, apenas log periódico
    }

    public void OnExiting()
    {
        try
        {
            _logSystem?.LogInfo("OnExiting called - flushing logs");
            _logSystem?.FlushLogs();
            _externalContent?.Unload();
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[CelesteGameActivity] ERROR during OnExiting: {ex}");
        }
    }

    public ILogSystem? GetLogSystem() => _logSystem;
}
