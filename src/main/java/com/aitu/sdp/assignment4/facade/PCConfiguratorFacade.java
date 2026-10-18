package com.aitu.sdp.assignment4.facade;

import com.aitu.sdp.assignment2.BudgetPCOrderFactory;
import com.aitu.sdp.assignment2.ComponentCatalog;
import com.aitu.sdp.assignment2.ComponentFactory;
import com.aitu.sdp.assignment2.Computer;
import com.aitu.sdp.assignment2.ComputerBuilder;
import com.aitu.sdp.assignment2.ComputerOrderFactory;
import com.aitu.sdp.assignment2.GamingComputerBuilder;
import com.aitu.sdp.assignment2.GamingPCOrderFactory;
import com.aitu.sdp.assignment2.PowerRequirementCalculator;
import com.aitu.sdp.assignment2.WorkstationPCOrderFactory;
import com.aitu.sdp.assignment2.controller.BuildPCRequest;
import com.aitu.sdp.assignment2.controller.CustomPCRequest;
import com.aitu.sdp.assignment2.controller.PCResponse;
import com.aitu.sdp.assignment4.diagnostics.BuildDiagnostics;
import com.aitu.sdp.assignment4.diagnostics.BuildDiagnosticsService;
import com.aitu.sdp.assignment4.diagnostics.DiagnosticStatus;
import com.aitu.sdp.assignment4.support.ComponentFactoryProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PCConfiguratorFacade {
    private final ComponentFactoryProvider factoryProvider;
    private final BuildDiagnosticsService diagnosticsService;

    public PCConfiguratorFacade(ComponentFactoryProvider factoryProvider,
                               BuildDiagnosticsService diagnosticsService) {
        this.factoryProvider = factoryProvider;
        this.diagnosticsService = diagnosticsService;
    }

    public List<PCResponse> getPresets() {
        ComponentFactory factory = factoryProvider.factoryFor("intel_nvidia");
        return List.of(
                response("Ultimate Gaming Beast",
                        new GamingPCOrderFactory(factory).orderComputer(), "gaming", "intel_nvidia"),
                response("Budget Office PC",
                        new BudgetPCOrderFactory(factory).orderComputer(), "budget", "intel_nvidia"),
                response("Creator Workstation",
                        new WorkstationPCOrderFactory(factory).orderComputer(), "workstation", "intel_nvidia"));
    }

    public Map<String, Object> getComponents(String ecosystem) {
        ComponentFactory factory = factoryProvider.factoryFor(ecosystem);
        var family = factory.family();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cpus", ComponentCatalog.cpus(family).stream()
                .map(spec -> Map.of("model", spec.model(), "tdpWatts", spec.tdpWatts(),
                        "socket", spec.socket())).toList());
        result.put("gpus", ComponentCatalog.gpus(family).stream()
                .map(spec -> Map.of("model", spec.model(), "tdpWatts", spec.tdpWatts())).toList());
        result.put("motherboards", ComponentCatalog.motherboards(family).stream()
                .map(spec -> Map.of("model", spec.model(), "socket", spec.socket())).toList());
        result.put("ram", ComponentCatalog.ramModels());
        result.put("storage", ComponentCatalog.storageModels());
        result.put("powerSupplies", ComponentCatalog.powerSupplyModels());
        result.put("coolingSystems", ComponentCatalog.coolingModels());
        return result;
    }

    public BuildDiagnostics diagnose(CustomPCRequest request) {
        return diagnosticsService.diagnose(request);
    }

    public PCResponse buildCustom(CustomPCRequest request) {
        BuildDiagnostics diagnostics = diagnosticsService.diagnose(request);
        requireBuildAllowed(diagnostics);
        Computer computer = buildComputer(request);
        return response("Custom Studio Build", computer, "custom",
                request.getEcosystem(), diagnostics);
    }

    public Computer buildComputerForReport(CustomPCRequest request) {
        BuildDiagnostics diagnostics = diagnosticsService.diagnose(request);
        requireBuildAllowed(diagnostics);
        return buildComputer(request);
    }

    private Computer buildComputer(CustomPCRequest request) {
        ComponentFactory factory = factoryProvider.factoryFor(request.getEcosystem());
        ComputerBuilder builder = new GamingComputerBuilder(factory)
                .cpu(factory.createCPU(request.getCpu().trim()))
                .gpu(factory.createGPU(request.getGpu().trim()))
                .motherboard(factory.createMotherboard(request.getMotherboard().trim()))
                .ram(request.getRam().trim())
                .storage(request.getStorage().trim())
                .powerSupply(factory.createPowerSupply(request.getPowerSupply().trim()))
                .withRGB(request.isHasRGB());
        if (request.getCoolingSystem() != null && !request.getCoolingSystem().isBlank()) {
            builder.coolingSystem(request.getCoolingSystem().trim());
        }
        return builder.build();
    }

    public PCResponse buildNamed(BuildPCRequest request) {
        validateBuildRequest(request);
        ComponentFactory factory = factoryProvider.factoryFor(request.getEcosystem());
        ComputerOrderFactory orderFactory = orderFactory(request.getBuildType(), factory);
        Computer computer = orderFactory.orderComputer(
                request.getRam().trim(), request.getStorage().trim(),
                request.getCoolingSystem(), request.isHasRGB());
        BuildDiagnostics diagnostics = diagnosticsService.diagnose(toRequest(
                request.getEcosystem(), computer));
        requireBuildAllowed(diagnostics);
        return response(request.getBuildType() + " build", computer, request.getBuildType(),
                request.getEcosystem(), diagnostics);
    }

    private ComputerOrderFactory orderFactory(String type, ComponentFactory factory) {
        return switch (type.trim().toLowerCase()) {
            case "gaming" -> new GamingPCOrderFactory(factory);
            case "budget" -> new BudgetPCOrderFactory(factory);
            case "workstation" -> new WorkstationPCOrderFactory(factory);
            default -> throw new IllegalArgumentException("buildType must be gaming, budget, or workstation");
        };
    }

    private void validateBuildRequest(BuildPCRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        requireText(request.getEcosystem(), "ecosystem");
        requireText(request.getBuildType(), "buildType");
        requireText(request.getRam(), "ram");
        requireText(request.getStorage(), "storage");
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
    }

    private void requireBuildAllowed(BuildDiagnostics diagnostics) {
        if (diagnostics.status() == DiagnosticStatus.BLOCKED || !diagnostics.buildAllowed()) {
            throw new BuildRejectedException(diagnostics);
        }
    }

    private PCResponse response(String name, Computer computer, String pipeline, String ecosystem) {
        BuildDiagnostics diagnostics = diagnosticsService.diagnose(toRequest(ecosystem, computer));
        return response(name, computer, pipeline, ecosystem, diagnostics);
    }

    private PCResponse response(String name, Computer computer, String pipeline,
                                String ecosystem, BuildDiagnostics diagnostics) {
        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("CPU", computer.getCpu());
        specs.put("GPU", computer.getGpu());
        specs.put("RAM", computer.getRam());
        specs.put("Storage", computer.getStorage());
        specs.put("Power Supply", computer.getPowerSupply());
        specs.put("Motherboard", computer.getMotherboard());
        specs.put("Cooling System", computer.getCoolingSystem());
        specs.put("RGB Lighting", computer.isHasRGB() ? "Enabled" : "Disabled");
        int required = PowerRequirementCalculator.requiredWatts(
                computer.getCpuComponent().getTdpWatts(), computer.getGpuComponent().getTdpWatts());
        String highlights = "Abstract Factory selected the " + ecosystem
                + " component family. Factory Method executed the " + pipeline
                + " pipeline. Builder created an immutable Computer after validation.";
        String validationStatus = diagnostics.status() == DiagnosticStatus.REVIEW
                ? "Valid - build allowed with recommendations."
                : diagnostics.status() == DiagnosticStatus.BLOCKED
                    ? "Blocked - incompatible configuration."
                    : "Valid - compatible configuration.";
        return new PCResponse(specs,
                validationStatus,
                highlights, name, required, PowerRequirementCalculator.recommendedWatts(required),
                computer.getPowerSupplyComponent().wattage(), diagnostics);
    }

    private CustomPCRequest toRequest(String ecosystem, Computer computer) {
        CustomPCRequest request = new CustomPCRequest();
        request.setEcosystem(ecosystem);
        request.setCpu(computer.getCpu());
        request.setGpu(computer.getGpu());
        request.setMotherboard(computer.getMotherboard());
        request.setRam(computer.getRam());
        request.setStorage(computer.getStorage());
        request.setPowerSupply(computer.getPowerSupply());
        request.setCoolingSystem(computer.getCoolingSystem());
        request.setHasRGB(computer.isHasRGB());
        return request;
    }
}
