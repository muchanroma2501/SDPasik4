package com.aitu.sdp.assignment4.facade;

import com.aitu.sdp.assignment4.diagnostics.BuildDiagnostics;

public class BuildRejectedException extends RuntimeException {
    private final BuildDiagnostics diagnostics;

    public BuildRejectedException(BuildDiagnostics diagnostics) {
        super("Конфигурация содержит ошибки совместимости и не может быть собрана.");
        this.diagnostics = diagnostics;
    }

    public BuildDiagnostics getDiagnostics() {
        return diagnostics;
    }
}
