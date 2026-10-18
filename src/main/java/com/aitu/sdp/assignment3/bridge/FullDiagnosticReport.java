package com.aitu.sdp.assignment3.bridge;

import com.aitu.sdp.assignment2.Computer;

public class  FullDiagnosticReport extends ComputerReport {
    public FullDiagnosticReport(ReportExporter exporter) {
        super(exporter);
    }

    @Override
    public String generate(Computer computer) {
        return generateReport("Full PC Diagnostic Report", computer, ReportDetail.FULL_DIAGNOSTIC);
    }
}
