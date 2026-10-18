package com.aitu.sdp.assignment4.facade;

import com.aitu.sdp.assignment2.controller.CustomPCRequest;

public record PCReportRequest(String reportType, String format, CustomPCRequest configuration) {
}
