# Assignment #4 Report — Draft

> Draft for the student to review, personalize, and align with the instructor's requirements.

## 1. Introduction

This project is a PC Build Studio web application. The user selects hardware in a browser. A Spring Boot server reads the component catalog, checks the configuration, builds a computer, and can export a report.

Assignment #4 adds the Facade pattern to the existing Builder, Factory Method, Abstract Factory, and Bridge implementations. The facade gives the controller one clear API for a multi-step operation.

## 2. Why the PC configurator needs a Facade

A custom build uses several parts of the application. The platform decides which component factory to use. The catalog resolves hardware models and specifications. Diagnostics checks required values, component families, sockets, and power. Finally, the Builder creates the immutable `Computer`.

Without a facade, the HTTP controller would need to know the order of these steps and how their errors are handled. `PCConfiguratorFacade` owns that coordination. The controller receives transport objects and delegates each operation.

## 3. UML class diagram

See [docs/uml.md](uml.md). The diagram shows the controllers, `PCConfiguratorFacade`, catalog/provider, Builder, and compatibility-diagnostics collaborators. It also shows the separate `PCReportFacade` and the Assignment 3 Bridge.

## 4. How the facade coordinates collaborators

For a custom configuration, the facade asks `BuildDiagnosticsService` to inspect the command first. That service checks required values, asks the `ComponentFactoryProvider` to validate the platform, resolves models from `ComponentCatalog`, and uses `CompatibilityChecker` to evaluate family, socket, PSU minimum, and power headroom.

The service calculates the educational power estimate with the shared domain `PowerRequirementCalculator`. A blocking error causes the facade to reject the build. A warning allows construction. The facade then selects the platform factory and configures the existing Builder. It returns the existing `PCResponse` fields together with structured diagnostics and the three PSU wattage values.

For named builds, the facade selects the existing gaming, budget, or workstation `ComputerOrderFactory`. For presets it uses these same existing order factories.

## 5. What the facade hides

The PC controller does not choose component factories, create Builders, resolve catalog models, or sequence diagnostic checks. It calls a small high-level API: `getPresets`, `getComponents`, `diagnose`, `buildCustom`, or `buildNamed`.

The facade does not hide or replace the pattern implementations. It coordinates them and presents their result as a configuration operation.

## 6. Directly usable subsystem classes

Advanced clients may use `ComponentCatalog`, `ComponentFactory`, the concrete platform factories, `ComputerBuilder`, concrete Builders, `ComputerOrderFactory` implementations, `BuildDiagnosticsService`, or the report Bridge classes directly. These classes do not depend on `PCConfiguratorFacade`; the facade depends on the subsystem instead.

The report flow has a separate `PCReportFacade`. It uses `PCConfiguratorFacade` to get a validated selected `Computer`, then chooses a Bridge report and exporter. This keeps report formatting separate from the main configuration API.

## 7. Clean Code principles applied

1. **Single Responsibility:** `PCConfiguratorController` maps HTTP endpoints; `BuildDiagnosticsService` performs diagnostic coordination; `CompatibilityChecker` makes individual compatibility decisions; the Builder constructs the product.
2. **Information hiding:** the controller does not receive factories or Builders. It sees facade methods and request/response DTOs.
3. **DRY:** `PowerRequirementCalculator` is used both by diagnostics and by `AbstractComputerBuilder`, so the minimum-power formula has one implementation.
4. **Fail-fast validation:** `PCConfiguratorFacade` does not build when diagnostics contain an error. The Builder also validates required values, socket fit, and minimum PSU capacity.
5. **Meaningful names and small types:** `DiagnosticCheck`, `BuildDiagnostics`, and `PowerRequirementCalculator` communicate distinct roles and typed results.
6. **Dependency inversion:** reports use the `ReportExporter` abstraction, so report content and output format can vary independently.
7. **Safe rendering:** the browser uses DOM text nodes; the server HTML exporter escapes values; the preview iframe is sandboxed.

## 8. Annotated excerpts from the final implementation

The excerpts below are from the actual final source files.

**A. The facade validates before it builds** — `PCConfiguratorFacade.java`:

```java
public PCResponse buildCustom(CustomPCRequest request) {
    BuildDiagnostics diagnostics = diagnosticsService.diagnose(request);
    requireBuildAllowed(diagnostics);
    Computer computer = buildComputer(request);
    return response("Custom Studio Build", computer, "custom",
            request.getEcosystem(), diagnostics);
}
```

The controller does not need to know how the diagnostic result is produced or how a PC is assembled.

**B. Diagnostics uses the real catalog and a shared power rule** — `BuildDiagnosticsService.java`:

```java
Integer requiredWatts = cpu != null && gpu != null
        ? PowerRequirementCalculator.requiredWatts(cpu.getTdpWatts(), gpu.getTdpWatts())
        : null;
Integer selectedWatts = resolvePsuWatts(request.getPowerSupply());
Integer recommendedWatts = requiredWatts == null
        ? null : PowerRequirementCalculator.recommendedWatts(requiredWatts);
```

The models are resolved from `ComponentCatalog`; unknown selections become checks rather than uncaught request failures.

**C. Both layers use one calculation** — `AbstractComputerBuilder.java`:

```java
int requiredPower = PowerRequirementCalculator.requiredWatts(
        cpu.getTdpWatts(), gpu.getTdpWatts());
if (requiredPower > selectedPower) {
    throw new IncompatibleHardwareException(...);
}
```

The Builder still has its own direct construction safeguards for advanced clients.

**D. The controller delegates to a small API** — `PCConfiguratorController.java`:

```java
@PostMapping("/diagnose")
public BuildDiagnostics diagnose(@RequestBody(required = false) CustomPCRequest request) {
    return configuratorFacade.diagnose(request);
}
```

There is no factory, catalog, or Builder orchestration in the controller.

**E. Report formatting is a separate facade** — `PCReportFacade.java`:

```java
Computer computer = configuratorFacade.buildComputerForReport(request.configuration());
return export(computer, request.reportType(), request.format());
```

The new POST report API therefore uses the user's actual selection and retains the Bridge implementation.

## 9. Conclusion

The Facade pattern makes the web client's build workflow simpler without removing the existing design patterns. The server validates the selection before construction, reports warnings separately from errors, and reuses the same educational power calculation in diagnostics and the Builder.

The diagnostic checks only claim rules supported by the catalog. CPU/motherboard socket, platform-family, and estimated PSU capacity are checked. RAM-generation, thermal, and performance validation are not claimed by this compatibility workflow.

## 10. Repository

<https://github.com/muchanroma2501/SDPasik2>
