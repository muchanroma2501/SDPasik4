package com.aitu.sdp.assignment2.controller;

import com.aitu.sdp.assignment4.diagnostics.BuildDiagnostics;
import com.aitu.sdp.assignment4.facade.PCConfiguratorFacade;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pc")
@CrossOrigin
public class PCConfiguratorController {
    private final PCConfiguratorFacade configuratorFacade;

    public PCConfiguratorController(PCConfiguratorFacade configuratorFacade) {
        this.configuratorFacade = configuratorFacade;
    }

    @GetMapping("/presets")
    public List<PCResponse> presets() {
        return configuratorFacade.getPresets();
    }

    @GetMapping("/catalog")
    public Map<String, Object> catalog(
            @RequestParam(name = "ecosystem", defaultValue = "intel_nvidia") String ecosystem) {
        return configuratorFacade.getComponents(ecosystem);
    }

    @PostMapping("/diagnose")
    public BuildDiagnostics diagnose(@RequestBody(required = false) CustomPCRequest request) {
        return configuratorFacade.diagnose(request);
    }

    @PostMapping("/custom")
    public PCResponse custom(@RequestBody CustomPCRequest request) {
        return configuratorFacade.buildCustom(request);
    }

    @PostMapping("/build")
    public PCResponse build(@RequestBody BuildPCRequest request) {
        return configuratorFacade.buildNamed(request);
    }
}
