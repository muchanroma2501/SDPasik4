package com.aitu.sdp.assignment4.diagnostics;

import com.aitu.sdp.assignment2.controller.CustomPCRequest;
import com.aitu.sdp.assignment4.facade.BuildRejectedException;
import com.aitu.sdp.assignment4.facade.PCConfiguratorFacade;
import com.aitu.sdp.assignment4.support.ComponentFactoryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildDiagnosticsServiceTest {
    private BuildDiagnosticsService diagnosticsService;
    private PCConfiguratorFacade facade;

    @BeforeEach
    void setUp() {
        ComponentFactoryProvider provider = new ComponentFactoryProvider();
        diagnosticsService = new BuildDiagnosticsService(provider, new CompatibilityChecker());
        facade = new PCConfiguratorFacade(provider, diagnosticsService);
    }

    @Test
    void resolvesCatalogModelsAndAllowsValidBuild() {
        BuildDiagnostics result = diagnosticsService.diagnose(configuration(
                "Intel Core i7-14700K", "NVIDIA GeForce RTX 4070 Ti 12GB", "Z790 ATX",
                "850W 80+ Gold"));

        assertEquals(DiagnosticStatus.READY, result.status());
        assertTrue(result.buildAllowed());
        assertTrue(result.checks().stream().anyMatch(check -> check.id().equals("socket")
                && check.severity() == DiagnosticSeverity.PASS));
        assertEquals(640, result.requiredPowerWatts());
        assertEquals(704, result.recommendedPowerWatts());
    }

    @Test
    void reportsCpuMotherboardSocketMismatch() {
        BuildDiagnostics result = diagnosticsService.diagnose(configuration(
                "Intel Core i5-13400", "NVIDIA GeForce RTX 3050 8GB", "B650 ATX",
                "850W 80+ Gold"));

        assertEquals(DiagnosticSeverity.ERROR, check(result, "socket").severity());
        assertTrue(check(result, "socket").message().contains("LGA1700"));
        assertFalse(result.buildAllowed());
    }

    @Test
    void blocksPsuBelowCalculatedMinimum() {
        BuildDiagnostics result = diagnosticsService.diagnose(configuration(
                "Intel Core i9-14900K", "NVIDIA GeForce RTX 4090 24GB", "Z790 ATX",
                "850W 80+ Gold"));

        assertEquals(853, result.requiredPowerWatts());
        assertEquals(DiagnosticSeverity.ERROR, check(result, "psu-capacity").severity());
        assertEquals(DiagnosticStatus.BLOCKED, result.status());
    }

    @Test
    void warnsWhenPsuMeetsMinimumButNotRecommendedHeadroom() {
        BuildDiagnostics result = diagnosticsService.diagnose(configuration(
                "Intel Core i7-14700K", "NVIDIA GeForce RTX 3050 8GB", "Z790 ATX",
                "500W 80+ Bronze"));

        assertEquals(485, result.requiredPowerWatts());
        assertEquals(534, result.recommendedPowerWatts());
        assertEquals(DiagnosticSeverity.PASS, check(result, "psu-capacity").severity());
        assertEquals(DiagnosticSeverity.WARNING, check(result, "psu-headroom").severity());
        assertEquals(DiagnosticStatus.REVIEW, result.status());
        assertTrue(result.buildAllowed());
    }

    @Test
    void passesPsuAtOrAboveRecommendedHeadroom() {
        BuildDiagnostics result = diagnosticsService.diagnose(configuration(
                "Intel Core i7-14700K", "NVIDIA GeForce RTX 3050 8GB", "Z790 ATX",
                "650W 80+ Gold"));

        assertEquals(DiagnosticSeverity.PASS, check(result, "psu-capacity").severity());
        assertEquals(DiagnosticSeverity.PASS, check(result, "psu-headroom").severity());
        assertEquals(DiagnosticStatus.READY, result.status());
    }

    @Test
    void reportsUnknownHardwareWithoutThrowing() {
        CustomPCRequest request = configuration("Not a catalog CPU", "NVIDIA GeForce RTX 3050 8GB",
                "Z790 ATX", "650W 80+ Gold");

        BuildDiagnostics result = diagnosticsService.diagnose(request);

        assertEquals(DiagnosticStatus.BLOCKED, result.status());
        assertTrue(result.checks().stream().anyMatch(check -> check.id().equals("cpu-model")
                && check.severity() == DiagnosticSeverity.ERROR));
        assertNotNull(result.checks());
    }

    @Test
    void reportsInvalidPlatformWithoutThrowing() {
        CustomPCRequest request = configuration("Intel Core i5-13400",
                "NVIDIA GeForce RTX 3050 8GB", "Z790 ATX", "650W 80+ Gold");
        request.setEcosystem("other");

        BuildDiagnostics result = diagnosticsService.diagnose(request);

        assertEquals(DiagnosticStatus.BLOCKED, result.status());
        assertTrue(result.checks().stream().anyMatch(check -> check.id().equals("platform-selection")
                && check.severity() == DiagnosticSeverity.ERROR));
    }

    @Test
    void facadeBuildsValidConfigurationAndRejectsBlockingDiagnostics() {
        var response = facade.buildCustom(configuration(
                "Intel Core i7-14700K", "NVIDIA GeForce RTX 4070 Ti 12GB", "Z790 ATX",
                "850W 80+ Gold"));
        assertEquals("Intel Core i7-14700K", response.specs().get("CPU"));
        assertTrue(response.diagnostics().buildAllowed());

        var warningBuild = facade.buildCustom(configuration(
                "Intel Core i7-14700K", "NVIDIA GeForce RTX 3050 8GB", "Z790 ATX",
                "500W 80+ Bronze"));
        assertEquals(DiagnosticStatus.REVIEW, warningBuild.diagnostics().status());
        assertTrue(warningBuild.validationStatus().contains("allowed with recommendations"));

        BuildRejectedException rejected = assertThrows(BuildRejectedException.class,
                () -> facade.buildCustom(configuration(
                        "Intel Core i9-14900K", "NVIDIA GeForce RTX 4090 24GB", "Z790 ATX",
                        "850W 80+ Gold")));
        assertEquals(DiagnosticStatus.BLOCKED, rejected.getDiagnostics().status());
    }

    private DiagnosticCheck check(BuildDiagnostics result, String id) {
        return result.checks().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }

    static CustomPCRequest configuration(String cpu, String gpu, String motherboard, String psu) {
        CustomPCRequest request = new CustomPCRequest();
        request.setEcosystem("intel_nvidia");
        request.setCpu(cpu);
        request.setGpu(gpu);
        request.setMotherboard(motherboard);
        request.setRam("32GB DDR5");
        request.setStorage("1TB NVMe SSD");
        request.setPowerSupply(psu);
        request.setCoolingSystem("280mm AIO Liquid Cooling");
        request.setHasRGB(true);
        return request;
    }
}
