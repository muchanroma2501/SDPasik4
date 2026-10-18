package com.aitu.sdp.assignment2;

/** Shared educational power estimate used by diagnostics and validated construction. */
public final class PowerRequirementCalculator {
    private static final int SYSTEM_ALLOWANCE_WATTS = 150;
    private static final double RECOMMENDED_HEADROOM = 1.10;

    private PowerRequirementCalculator() {
    }

    public static int requiredWatts(int cpuTdpWatts, int gpuTdpWatts) {
        if (cpuTdpWatts < 0 || gpuTdpWatts < 0) {
            throw new IllegalArgumentException("Component power cannot be negative");
        }
        return cpuTdpWatts + gpuTdpWatts + SYSTEM_ALLOWANCE_WATTS;
    }

    public static int recommendedWatts(int requiredWatts) {
        if (requiredWatts < 0) {
            throw new IllegalArgumentException("Required power cannot be negative");
        }
        return (int) Math.ceil(requiredWatts * RECOMMENDED_HEADROOM);
    }
}
