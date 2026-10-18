package com.aitu.sdp.assignment3.controller;

import com.aitu.sdp.assignment4.facade.PCReportFacade;
import com.aitu.sdp.assignment4.facade.PCReportRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class BridgeReportController {
    private final PCReportFacade reportFacade;

    public BridgeReportController(PCReportFacade reportFacade) {
        this.reportFacade = reportFacade;
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam(name = "reportType", defaultValue = "summary") String reportType,
            @RequestParam(name = "format", defaultValue = "json") String format) {
        return response(reportFacade.exportLegacy(reportType, format));
    }

    @PostMapping("/export")
    public ResponseEntity<String> exportSelected(@RequestBody PCReportRequest request) {
        return response(reportFacade.exportSelected(request));
    }

    private ResponseEntity<String> response(PCReportFacade.ExportedReport report) {
        return ResponseEntity.ok().contentType(report.contentType()).body(report.body());
    }
}
