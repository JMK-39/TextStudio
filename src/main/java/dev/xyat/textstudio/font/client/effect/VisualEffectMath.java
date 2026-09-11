package dev.xyat.textstudio.font.client.effect;

public final class VisualEffectMath {
    private VisualEffectMath() {
    }

    public static float sweepStrength(long time, int index, int length, double speed, double width) {
        int safeLength = Math.max(1, length);
        double safeSpeed = Math.max(0.05, speed);
        double safeWidth = Math.max(0.25, width);
        double cycle = safeLength + safeWidth * 2.0;
        double center = (time * 0.001 * safeSpeed * 6.0) % cycle - safeWidth;
        double distance = Math.abs(index - center);
        if (distance >= safeWidth) {
            return 0.0f;
        }
        double t = 1.0 - distance / safeWidth;
        return (float) (t * t * (3.0 - 2.0 * t));
    }

    public static float trailX(float x, float baseX, float strength) {
        return (baseX - x) * Math.max(0.0f, strength);
    }

    public static float trailY(float y, float baseY, float strength) {
        return (baseY - y) * Math.max(0.0f, strength);
    }
}
