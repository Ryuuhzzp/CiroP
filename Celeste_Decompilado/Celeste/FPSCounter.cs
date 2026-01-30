using Microsoft.Xna.Framework;
using Microsoft.Xna.Framework.Graphics;

namespace Celeste
{
    public class FPSCounter
    {
        private double _elapsedTime;
        private int _frameCount;
        private int _fps;
        private SpriteFont _font;

        public int CurrentFPS => _fps;

        public FPSCounter(SpriteFont font)
        {
            _font = font;
            _frameCount = 0;
            _fps = 0;
            _elapsedTime = 0.0;
        }

        public void Update(GameTime gameTime)
        {
            _elapsedTime += gameTime.ElapsedGameTime.TotalSeconds;
            _frameCount++;

            if (_elapsedTime >= 1.0)
            {
                _fps = _frameCount;
                _frameCount = 0;
                _elapsedTime -= 1.0;
            }
        }

        public void Draw(SpriteBatch spriteBatch, Vector2 position, Color color)
        {
            if (_font != null)
            {
                string text = $"FPS: {_fps}";
                spriteBatch.DrawString(_font, text, position, color);
            }
        }

        public void SetFont(SpriteFont font)
        {
            _font = font;
        }
    }
}
