package dev.xyat.textstudio.font.client.effect;

public final class AdvancedGlyphMath {
    private AdvancedGlyphMath() {
    }

    public static float scale(long time, int index, double amplitude, double speed) {
        double safeSpeed = Math.max(0.1, speed);
        return 1.0f + (float) Math.sin(time * safeSpeed * 0.003 + index * 0.4) * (float) amplitude;
    }

    public static float ecgOffset(long time, int index, double amplitude, double speed, double width) {
        return ecgOffset(time, index, 40, amplitude, speed, width);
    }

    public static float ecgOffset(long time, int index, int length, double amplitude, double speed, double width) {
        double safeSpeed = Math.max(0.05, speed);
        double safeWidth = Math.max(0.25, width);
        int safeLength = Math.max(1, length);
        double cycle = safeLength + safeWidth;
        double sweep = (time * safeSpeed * 0.004) % cycle;
        double distance = Math.abs(index - sweep);
        if (distance >= safeWidth) {
            return 0.0f;
        }
        double normalized = distance / safeWidth;
        double pulse = 0.5 + 0.5 * Math.cos(normalized * Math.PI);
        return (float) (-pulse * 5.0 * amplitude);
    }

    public static float heartbeatOffset(long time, int index, double amplitude, double speed) {
        double safeSpeed = Math.max(0.05, speed);
        double phase = time * 0.005 * safeSpeed - index * 0.08;
        double pulse = 0.5 + 0.5 * Math.sin(phase);
        return (float) (-Math.pow(pulse, 16.0) * 4.5 * amplitude);
    }

    public static boolean signalLoss(long time, int index, double speed, double chance) {
        double safeSpeed = Math.max(0.05, speed);
        double safeChance = clamp(chance, 0.0, 1.0);
        long bucket = (long) (time * safeSpeed / 50.0);
        long seed = mix(bucket * 7L + index * 3L);
        return unit(seed) < safeChance;
    }

    public static float glitchSpikeX(long time, int index, double amplitude, double speed, double chance) {
        double safeSpeed = Math.max(0.05, speed);
        double safeChance = clamp(chance, 0.0, 1.0);
        long bucket = (long) (time * safeSpeed / 50.0);
        long seed = mix(bucket * 31L + index * 17L);
        if (unit(seed) >= safeChance) {
            return 0.0f;
        }
        return (unit(mix(seed ^ 0x3243F6A8885A308DL)) - 0.5f) * 10.0f * (float) amplitude;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long mix(long value) {
        value ^= value >>> 12;
        value ^= value << 25;
        value ^= value >>> 27;
        return value * 0x2545F4914F6CDD1DL;
    }

    private static float unit(long value) {
        return (float) ((value >>> 11) * 0x1.0p-53);
    }
}
