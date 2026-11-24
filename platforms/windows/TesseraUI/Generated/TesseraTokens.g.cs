// TesseraUI v0.6.1 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
using System;
using System.Numerics;

namespace TesseraUI
{
    /// <summary>弹簧参数。WinUI Composition 的 SpringNaturalMotionAnimation 使用 DampingRatio + Period，Period 即本库的“时长”。</summary>
    public readonly record struct TesseraSpring(float DampingRatio, TimeSpan Period, float Stiffness, float Damping);

    public static class TesseraMotion
    {
        public static readonly TesseraSpring Snappy = new(0.85f, TimeSpan.FromMilliseconds(300), 438.6f, 35.6f);
        public static readonly TesseraSpring Smooth = new(1f, TimeSpan.FromMilliseconds(400), 246.7f, 31.4f);
        public static readonly TesseraSpring Bouncy = new(0.75f, TimeSpan.FromMilliseconds(500), 157.9f, 18.8f);
        public static readonly TimeSpan DurationInstant = TimeSpan.FromMilliseconds(100);
        public static readonly TimeSpan DurationFast = TimeSpan.FromMilliseconds(150);
        public static readonly TimeSpan DurationNormal = TimeSpan.FromMilliseconds(250);
        public static readonly TimeSpan DurationSlow = TimeSpan.FromMilliseconds(350);
        public static readonly (Vector2 P1, Vector2 P2) EaseStandard = (new(0.2f, 0f), new(0f, 1f));
        public static readonly (Vector2 P1, Vector2 P2) EaseEnter = (new(0.05f, 0.7f), new(0.1f, 1f));
        public static readonly (Vector2 P1, Vector2 P2) EaseExit = (new(0.3f, 0f), new(0.8f, 0.15f));
        /// <summary>按下时砖往下沉的距离（epx）</summary>
        public const float PressSink = 1f;
        public const int PressDurationMs = 80;
        public const float SettleLift = 6f;
        public const float SettleScale = 1.015f;
        public const float FlingVelocity = 500f;
        public const int StaggerStepMs = 30;
        public const int StaggerMaxItems = 8;
        public const int ToastDurationMs = 3000;
        public const int ToastDurationWithActionMs = 5000;
    }

    /// <summary>流韵外观的动效数值与启用条件</summary>
    public static class TesseraFlowLimits
    {
        public const int MinCores = 6;
        public const int MinMemoryGB = 4;
        public const float PressScale = 0.96f;
        public const int RippleDurationMs = 760;
        public const int SplashDurationMs = 640;
    }

    public static class TesseraZ
    {
        public const int Sticky = 100;
        public const int Overlay = 200;
        public const int Modal = 300;
        public const int Popover = 400;
        public const int Toast = 500;
        public const int Tooltip = 600;
    }
}
