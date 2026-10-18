package com.aitu.sdp.assignment4.facade;

import com.aitu.sdp.assignment2.Computer;
import org.springframework.stereotype.Component;

/**
 * Manages thermal sensor state and cooling-fan mode independently of the facade.
 */
@Component
public class ThermalMonitoringSubsystem {
    private static final double SAFE_TEMPERATURE_CELSIUS = 90;
    private static final double CRITICAL_TEMPERATURE_CELSIUS = 110;
    private static final double BASE_TEMPERATURE_CELSIUS = 30;
    private static final double TEMPERATURE_PER_TDP_WATT = 0.1;
    private static final double MAX_FAN_COOLING_CELSIUS = 18;

    private Computer monitoredComputer;
    private boolean sensorsInitialized;
    private boolean fansAtMaximum;

    public synchronized void initializeSensors(Computer computer) {
        if (computer == null) {
            throw new IllegalArgumentException("Computer cannot be null");
        }
        monitoredComputer = computer;
        sensorsInitialized = true;
        fansAtMaximum = false;
    }

    public synchronized void scaleFansToMaximum() {
        requireInitializedSensors();
        fansAtMaximum = true;
    }

    /**
     * Returns whether the estimated temperature is safe and throws for critical heat.
     */
    public synchronized boolean verifySafeTemperatures() {
        requireInitializedSensors();
        int combinedTdp = monitoredComputer.getCpuComponent().getTdpWatts()
                + monitoredComputer.getGpuComponent().getTdpWatts();
        double estimatedTemperature = BASE_TEMPERATURE_CELSIUS
                + combinedTdp * TEMPERATURE_PER_TDP_WATT
                - (fansAtMaximum ? MAX_FAN_COOLING_CELSIUS : 0);

        if (estimatedTemperature >= CRITICAL_TEMPERATURE_CELSIUS) {
            throw new SubsystemOverheatException(String.format(
                    "Critical temperature detected: %.1f C", estimatedTemperature));
        }
        return estimatedTemperature < SAFE_TEMPERATURE_CELSIUS;
    }

    /** Restores quiet cooling; safe to call repeatedly, including during error handling. */
    public synchronized void restoreQuietFanMode() {
        fansAtMaximum = false;
    }

    private void requireInitializedSensors() {
        if (!sensorsInitialized) {
            throw new IllegalStateException("Thermal sensors have not been initialized");
        }
    }
}
