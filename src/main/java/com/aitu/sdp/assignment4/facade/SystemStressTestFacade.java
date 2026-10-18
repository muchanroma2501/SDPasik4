package com.aitu.sdp.assignment4.facade;

import com.aitu.sdp.assignment2.Computer;
import com.aitu.sdp.assignment3.bridge.HtmlReportExporter;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Presents one simple diagnostics API while coordinating the independent subsystems.
 *
 * <p>Clean Code: the name and public method describe the operation without exposing
 * implementation details. The facade follows SRP by coordinating only; benchmark,
 * rendering, thermal control, and HTML rendering remain with focused collaborators.
 * Subsystems are kept out of the client-facing API, while remaining independently
 * usable by advanced clients. The serialized operation protects the stateful thermal
 * subsystem from overlapping diagnostic runs.</p>
 */
@Service
public class SystemStressTestFacade {
    private final CPUBenchmarkRunner cpuBenchmarkRunner;
    private final GPURenderTester gpuRenderTester;
    private final ThermalMonitoringSubsystem thermalMonitoringSubsystem;
    private final HtmlReportExporter htmlReportExporter;

    public SystemStressTestFacade(CPUBenchmarkRunner cpuBenchmarkRunner,
                                  GPURenderTester gpuRenderTester,
                                  ThermalMonitoringSubsystem thermalMonitoringSubsystem,
                                  HtmlReportExporter htmlReportExporter) {
        this.cpuBenchmarkRunner = cpuBenchmarkRunner;
        this.gpuRenderTester = gpuRenderTester;
        this.thermalMonitoringSubsystem = thermalMonitoringSubsystem;
        this.htmlReportExporter = htmlReportExporter;
    }

    /**
     * Runs CPU and GPU diagnostics under maximum cooling and returns an HTML report.
     *
     * <p>The thermal check is enclosed in a {@code finally} block so quiet fan mode
     * is restored before any subsystem failure, including overheat, leaves this API.</p>
     *
     * @param computer the fully assembled computer to diagnose
     * @return an HTML report containing component specifications and benchmark scores
     * @throws IllegalArgumentException if {@code computer} is null
     * @throws SubsystemOverheatException if operating temperature is unsafe or critical
     */
    public synchronized String executeSystemDiagnostics(Computer computer) {
        if (computer == null) {
            throw new IllegalArgumentException("Computer cannot be null");
        }

        double cpuScore;
        double gpuScore;
        thermalMonitoringSubsystem.initializeSensors(computer);
        try {
            thermalMonitoringSubsystem.scaleFansToMaximum();
            cpuScore = cpuBenchmarkRunner.runBenchmark(computer.getCpuComponent());
            gpuScore = gpuRenderTester.runRenderTest(computer.getGpuComponent());
            if (!thermalMonitoringSubsystem.verifySafeTemperatures()) {
                throw new SubsystemOverheatException(
                        "Operating temperature exceeded the safe threshold");
            }
        } finally {
            thermalMonitoringSubsystem.restoreQuietFanMode();
        }

        Map<String, String> diagnosticData = new LinkedHashMap<>();
        diagnosticData.put("CPU benchmark score (operations/second)", formatScore(cpuScore));
        diagnosticData.put("GPU render score (shader operations/second)", formatScore(gpuScore));
        diagnosticData.put("Thermal status", "Safe");
        diagnosticData.put("Cooling mode after test", "Quiet");
        return htmlReportExporter.exportDiagnosticReport(computer, diagnosticData);
    }

    private String formatScore(double score) {
        return String.format(Locale.ROOT, "%.2f", score);
    }
}
