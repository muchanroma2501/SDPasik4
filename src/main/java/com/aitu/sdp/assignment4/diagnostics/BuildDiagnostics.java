package com.aitu.sdp.assignment4.diagnostics;

import java.util.List;

public record BuildDiagnostics(
        DiagnosticStatus status,
        List<DiagnosticCheck> checks,
        Integer requiredPowerWatts,
        Integer recommendedPowerWatts,
        Integer selectedPowerWatts,
        boolean buildAllowed) {
}
