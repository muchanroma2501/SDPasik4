package com.aitu.sdp.assignment4.facade;

import com.aitu.sdp.assignment2.Computer;
import com.aitu.sdp.assignment2.GamingPCOrderFactory;
import com.aitu.sdp.assignment3.bridge.ComputerReport;
import com.aitu.sdp.assignment3.bridge.FullDiagnosticReport;
import com.aitu.sdp.assignment3.bridge.HtmlReportExporter;
import com.aitu.sdp.assignment3.bridge.JsonReportExporter;
import com.aitu.sdp.assignment3.bridge.ReportExporter;
import com.aitu.sdp.assignment3.bridge.SummarySpecsReport;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class PCReportFacade {
    private final PCConfiguratorFacade configuratorFacade;
    private final HtmlReportExporter htmlReportExporter;
    private final JsonReportExporter jsonReportExporter;

    public PCReportFacade(PCConfiguratorFacade configuratorFacade,
                          HtmlReportExporter htmlReportExporter,
                          JsonReportExporter jsonReportExporter) {
        this.configuratorFacade = configuratorFacade;
        this.htmlReportExporter = htmlReportExporter;
        this.jsonReportExporter = jsonReportExporter;
    }

    public ExportedReport exportSelected(PCReportRequest request) {
        validateRequest(request);
        Computer computer = configuratorFacade.buildComputerForReport(request.configuration());
        return export(computer, request.reportType(), request.format());
    }

    public ExportedReport exportLegacy(String reportType, String format) {
        Computer computer = new GamingPCOrderFactory().orderComputer();
        return export(computer, reportType, format);
    }

    private ExportedReport export(Computer computer, String reportType, String format) {
        String normalizedType = normalize(reportType);
        String normalizedFormat = normalize(format);
        ReportExporter exporter = switch (normalizedFormat) {
            case "html" -> htmlReportExporter;
            case "json" -> jsonReportExporter;
            default -> throw new IllegalArgumentException("format must be 'json' or 'html'");
        };
        ComputerReport report = switch (normalizedType) {
            case "summary" -> new SummarySpecsReport(exporter);
            case "full" -> new FullDiagnosticReport(exporter);
            default -> throw new IllegalArgumentException("reportType must be 'summary' or 'full'");
        };
        MediaType contentType = "html".equals(normalizedFormat)
                ? MediaType.TEXT_HTML : MediaType.APPLICATION_JSON;
        return new ExportedReport(contentType, report.generate(computer));
    }

    private void validateRequest(PCReportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.configuration() == null) {
            throw new IllegalArgumentException("configuration is required");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    public record ExportedReport(MediaType contentType, String body) {
    }
}
