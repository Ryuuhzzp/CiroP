import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(CelesteApp());

class CelesteApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Celeste (Port)',
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.red),
      home: CelesteHomePage(),
    );
  }
}

class CelesteHomePage extends StatefulWidget {
  @override
  _CelesteHomePageState createState() => _CelesteHomePageState();
}

class _CelesteHomePageState extends State<CelesteHomePage> {
  static const _channel = MethodChannel('celestemeown.app/channel');

  bool _assetsInstalled = false;
  bool _assetsInstalling = false;
  bool _fpsEnabled = false;
  bool _verboseLogs = false;

  Future<dynamic> _callNative(String method, [dynamic args]) async {
    try {
      return await _channel.invokeMethod(method, args);
    } on PlatformException catch (e) {
      _showSnack('Erro nativo: ${e.message}');
    } catch (e) {
      _showSnack('Erro: $e');
    }
  }

  void _showSnack(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  Future<void> _installAssets() async {
    setState(() => _assetsInstalling = true);
    final res = await _callNative('installAssets');
    setState(() {
      _assetsInstalling = false;
      _assetsInstalled = res == true || res == 'ok';
    });
    _showSnack(_assetsInstalled ? 'Assets instalados' : 'Instalação finalizada');
  }

  Future<void> _startGame() async {
    if (!_assetsInstalled) {
      _showSnack('Instale os assets antes de iniciar o jogo');
      return;
    }
    await _callNative('startGame');
  }

  Future<void> _toggleFps(bool enabled) async {
    setState(() => _fpsEnabled = enabled);
    await _callNative('setFpsEnabled', enabled);
  }

  Future<void> _toggleVerbose(bool enabled) async {
    setState(() => _verboseLogs = enabled);
    await _callNative('setVerboseLogs', enabled);
  }

  Future<void> _exportLogs() async {
    await _callNative('exportLogs');
  }

  Future<void> _exportScreenshot() async {
    await _callNative('exportScreenshot');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Celeste (Port)')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Status de assets: ${_assetsInstalled ? 'Instalados' : 'Não instalados'}'),
            const SizedBox(height: 8),
            ElevatedButton(
              onPressed: _assetsInstalling ? null : _installAssets,
              child: Text(_assetsInstalling ? 'Instalando...' : 'Instalar Assets'),
            ),
            const SizedBox(height: 12),
            ElevatedButton(onPressed: _startGame, child: const Text('Iniciar Jogo')),
            const Divider(height: 24),
            SwitchListTile(
              title: const Text('Mostrar FPS'),
              value: _fpsEnabled,
              onChanged: _toggleFps,
            ),
            SwitchListTile(
              title: const Text('Logs verbosos'),
              value: _verboseLogs,
              onChanged: _toggleVerbose,
            ),
            const SizedBox(height: 12),
            ElevatedButton(onPressed: _exportLogs, child: const Text('Exportar logs')),
            const SizedBox(height: 8),
            ElevatedButton(onPressed: _exportScreenshot, child: const Text('Exportar screenshot')),
            const Spacer(),
            Text('Canal nativo: celestemeown.app/channel', textAlign: TextAlign.center, style: TextStyle(fontSize: 12, color: Colors.grey[600])),
          ],
        ),
      ),
    );
  }
}
