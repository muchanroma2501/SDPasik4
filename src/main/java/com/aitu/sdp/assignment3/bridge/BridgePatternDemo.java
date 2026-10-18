package com.aitu.sdp.assignment3.bridge;

import com.aitu.sdp.assignment2.Computer;
import com.aitu.sdp.assignment2.GamingPCOrderFactory;

/** Standalone demonstration; it is intentionally not run during web application startup. */
public class BridgePatternDemo {
    public static void main(String[] args) {
        Computer computer = new GamingPCOrderFactory().orderComputer();
        ComputerReport report = new SummarySpecsReport(new JsonReportExporter());

        System.out.println("Bridge pattern demo - JSON exporter:");
        System.out.println(report.generate(computer));

        report.setExporter(new HtmlReportExporter());
        System.out.println("Bridge pattern demo - switched to HTML exporter:");
        System.out.println(report.generate(computer));
    }
}
