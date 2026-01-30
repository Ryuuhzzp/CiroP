# PRÓXIMAS AÇÕES IMEDIATAS (PRIORIDADE ALTA)

## Crítico - Bloqueia Progresso

1. **Instalar workload net8.0-android**
   ```bash
   dotnet workload restore
   dotnet workload install android
   ```
   Motivo: Necessário para TargetFramework net8.0-android34.0 e integração real MonoGame

2. **Baixar + Extrair FMOD Android ARM64**
   - Link: https://github.com/portceleste8-sketch/CELESTE-GAME-ANDROID-/releases/download/V1/fmodstudioapi20312android.tar.gz
   - Extrair libfmod.so + libfmodstudio.so
   - Copiar para: src/Celeste.Android/Properties/Android/jniLibs/arm64-v8a/
   - Registrar em RELATORIO.md

3. **Integração Flutter Add-to-App**
   - Criar settings.gradle + local.properties
   - Configurar pubspec.yaml com Flutter module (add-to-app mode)
   - Integrar FlutterEngine em MainActivity.kt
   - Registrar MethodChannel real

## Alto - Funcionalidade Core

4. **Asset Installer Kotlin**
   - Implementar AssetInstaller.kt
   - Download robusto (timeout, retry)
   - Unzip seguro (Zip Slip protection)
   - CheckContent forte (valida Dialogs/, Fonts/, Effects/, Atlases/, FMOD/Banks/)
   - SharedPreferences: assets_installed, timestamp, last_error

5. **ExternalFileContentManager Funcional**
   - Testar carga de XNBs do filesystem
   - Testar carga de PNGs/Fonts diretos
   - Integrar com VirtualTexture.cs

6. **Audio.cs Adaptar para Android**
   - Mudar FMOD/Desktop → FMOD/Android (ou path instalado)
   - Testar init de FMOD com arm64 .so

## Médio - UX/Polish

7. **Fullscreen + Landscape Bloqueado**
   - Testar immersive sticky onResume/onWindowFocusChanged
   - Validar landscape lock em ambas Activities

8. **LogSystem Captura Crashes**
   - Testar AppDomain.CurrentDomain.UnhandledException
   - Validar flush no crash
   - Testar exportação de logs via SAF

9. **FPS Counter Overlay**
   - Implementar desenho de FPS no canto
   - Toggle via UI Flutter → SharedPreferences → Intent flag

## Testes Obrigatórios

- Build Release passa (sem erros)
- APK publisha com arm64-v8a only
- Primeira execução: instala Content.zip
- CheckContent valida assets
- "Iniciar Jogo" só funciona se assets validados
- Jogo inicia em fullscreen landscape
- Logs são salvos em Logs/YYYY-MM-DD/session_*.log
- Crash logs são capturados em Logs/YYYY-MM-DD/crash_*.log

## Checklist Bloqueantes Finais

- [ ] Workload Android instalado
- [ ] FMOD arm64 em jniLibs/
- [ ] Content.zip download funcionando
- [ ] CheckContent validando
- [ ] Jogo iniciando via "Iniciar Jogo"
- [ ] Fullscreen/Landscape funcional
- [ ] Logs persistentes
- [ ] APK arm64-v8a only
