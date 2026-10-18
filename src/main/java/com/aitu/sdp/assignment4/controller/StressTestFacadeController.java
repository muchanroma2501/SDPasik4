package com.aitu.sdp.assignment4.controller;

import com.aitu.sdp.assignment2.controller.CustomPCRequest;
import com.aitu.sdp.assignment4.facade.SubsystemOverheatException;
import com.aitu.sdp.assignment4.facade.PCConfiguratorFacade;
import com.aitu.sdp.assignment4.facade.SystemStressTestFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


/**
 * HTTP client adapter for the stress-test facade; it never coordinates subsystems.
 */
@RestController
@RequestMapping("/api/diagnostics")
public class StressTestFacadeController {
    private final SystemStressTestFacade systemStressTestFacade;
    private final PCConfiguratorFacade configuratorFacade;

    public StressTestFacadeController(SystemStressTestFacade systemStressTestFacade,
                                      PCConfiguratorFacade configuratorFacade) {
        this.systemStressTestFacade = systemStressTestFacade;
        this.configuratorFacade = configuratorFacade;
    }

    /**
     * Runs diagnostics against the exact component selections used by the configurator.
     */
    @PostMapping(value = "/run", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> runDiagnostics(@RequestBody CustomPCRequest request) {
        var computer = configuratorFacade.buildComputerForReport(request);
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(systemStressTestFacade.executeSystemDiagnostics(computer));
        } catch (SubsystemOverheatException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    exception.getMessage(), exception);
        }
    }

}
