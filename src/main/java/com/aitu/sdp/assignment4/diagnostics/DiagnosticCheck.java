package com.aitu.sdp.assignment4.diagnostics;

public record DiagnosticCheck(
        String id,
        String title,
        DiagnosticSeverity severity,
        String message,
        String recommendation) {
}
