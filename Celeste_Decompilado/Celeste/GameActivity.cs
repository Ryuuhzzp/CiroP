using Android.App;
using Android.Content.PM;
using Android.OS;
using Microsoft.Xna.Framework;

namespace Celeste
{
    [Activity(
        Label = "Celeste",
        MainLauncher = false,
        Icon = "@mipmap/icon",
        Theme = "@style/MainTheme",
        ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation | ConfigChanges.Keyboard | ConfigChanges.KeyboardHidden,
        ScreenOrientation = ScreenOrientation.Landscape)]
    public class GameActivity : AndroidGameActivity
    {
        private Game _game;
        private FPSCounter _fpsCounter;
        private bool _fpsEnabled = false;

        protected override void OnCreate(Bundle savedInstanceState)
        {
            base.OnCreate(savedInstanceState);

            // Obter configurações do MethodChannel
            var sharedPrefs = GetSharedPreferences("celeste_preferences", FileCreationMode.Private);
            _fpsEnabled = sharedPrefs.GetBoolean("fps_enabled", false);

            _game = new CelesteGame();
            SetContentView((View)_game.Services.GetService(typeof(View)));
            
            // Inicializar FPS Counter se habilitado
            if (_fpsEnabled)
            {
                _fpsCounter = new FPSCounter(null); // Font será carregada do jogo
            }

            _game.Run();
        }

        public FPSCounter GetFpsCounter()
        {
            return _fpsCounter;
        }

        public bool IsFpsEnabled()
        {
            return _fpsEnabled;
        }
    }
}