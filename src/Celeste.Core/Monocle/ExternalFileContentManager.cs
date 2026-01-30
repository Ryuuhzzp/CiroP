using System;
using System.Collections.Generic;
using System.IO;
using Microsoft.Xna.Framework.Content;
using Monocle;

namespace Celeste;

/// <summary>
/// Implementação de IExternalContentManager para carregar assets do filesystem no Android.
/// Delega XNBs ao ContentManager do Engine, mas oferece fallback para arquivos diretos.
/// </summary>
public class ExternalFileContentManager : IExternalContentManager
{
    private readonly string _contentRoot;
    private readonly Dictionary<string, object> _cache = new Dictionary<string, object>(StringComparer.OrdinalIgnoreCase);

    public ExternalFileContentManager(string contentRoot)
    {
        _contentRoot = contentRoot ?? throw new ArgumentNullException(nameof(contentRoot));
    }

    public T Load<T>(string assetName) where T : class
    {
        if (string.IsNullOrEmpty(assetName))
            return default;

        // Normaliza assetName e previne path traversal
        string safeName = assetName.Replace('\\', '/').TrimStart('/');
        if (safeName.Contains(".."))
            throw new InvalidOperationException("Invalid asset name");

        // Primeiro, cache
        if (_cache.TryGetValue(safeName, out object cached) && cached is T t)
            return t;

        // Tentar com o Engine.Instance.Content (para XNBs)
        try
        {
            if (Engine.Instance != null)
            {
                var result = Engine.Instance.Content.Load<T>(assetName);
                if (result != null)
                {
                    _cache[safeName] = result;
                    return result;
                }
            }
        }
        catch
        {
            // Fallback para arquivo direto
        }

        // Fallback: procurar arquivo direto no filesystem
        string[] possiblePaths = new string[] { 
            Path.Combine(_contentRoot, safeName),
            Path.Combine(_contentRoot, safeName + ".xnb"),
            Path.Combine(_contentRoot, safeName + ".png"),
            Path.Combine(_contentRoot, safeName + ".fnt"),
            Path.Combine(_contentRoot, safeName + ".xml")
        };

        foreach (var p in possiblePaths)
        {
            if (File.Exists(p))
            {
                // Se for Stream, retornar FileStream
                if (typeof(T) == typeof(Stream))
                {
                    var fs = File.OpenRead(p);
                    _cache[safeName] = fs;
                    return fs as T;
                }

                // Se for string (texto), carregar conteúdo de texto
                if (typeof(T) == typeof(string))
                {
                    var text = File.ReadAllText(p);
                    _cache[safeName] = text;
                    return text as T;
                }

                // Outros tipos: tentar fallback do Engine novamente
                return default;
            }
        }

        return default;
    }

    public void Unload()
    {
        foreach (var v in _cache.Values)
        {
            if (v is Stream s)
            {
                try { s.Dispose(); } catch { }
            }
        }
        _cache.Clear();
    }
}
