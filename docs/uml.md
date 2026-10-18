# PC Build Studio — UML

The diagram reflects the implemented Assignment #4 configuration facade, diagnostic collaborators, Assignment 2 construction patterns, and the separate report facade.

```mermaid
classDiagram
    class PCConfiguratorController {
        +presets()
        +catalog(ecosystem)
        +diagnose(request)
        +custom(request)
        +build(request)
    }
    class PCConfiguratorFacade {
        +getPresets()
        +getComponents(ecosystem)
        +diagnose(request)
        +buildCustom(request)
        +buildNamed(request)
        +buildComputerForReport(request)
    }
    class ComponentFactoryProvider {
        +factoryFor(ecosystem)
        +familyFor(ecosystem)
    }
    class ComponentCatalog {
        <<catalog>>
    }
    class BuildDiagnosticsService {
        +diagnose(request) BuildDiagnostics
    }
    class CompatibilityChecker {
        +platformCheck(compatible)
        +socketCheck(cpu, motherboard)
        +powerCheck(selected, required)
        +headroomCheck(selected, recommended)
    }
    class PowerRequirementCalculator {
        <<domain rule>>
        +requiredWatts(cpuTdp, gpuTdp)
        +recommendedWatts(required)
    }
    class ComponentFactory {
        <<interface>>
    }
    class IntelNvidiaComponentFactory
    class AMDComponentFactory
    class ComputerBuilder {
        <<interface>>
    }
    class GamingComputerBuilder
    class ComputerOrderFactory {
        <<abstract Factory Method creator>>
    }
    class Computer
    class PCResponse
    class BuildDiagnostics

    class BridgeReportController
    class PCReportFacade {
        +exportSelected(request)
        +exportLegacy(reportType, format)
    }
    class ComputerReport {
        <<Bridge abstraction>>
    }
    class SummarySpecsReport
    class FullDiagnosticReport
    class ReportExporter {
        <<Bridge implementor>>
    }
    class HtmlReportExporter
    class JsonReportExporter

    PCConfiguratorController --> PCConfiguratorFacade : delegates requests
    PCConfiguratorController ..> PCResponse : transport DTO
    PCConfiguratorFacade --> ComponentFactoryProvider : selects platform factory
    PCConfiguratorFacade --> ComponentCatalog : gets component options
    PCConfiguratorFacade --> BuildDiagnosticsService : validates selections
    PCConfiguratorFacade --> ComputerOrderFactory : builds named presets
    PCConfiguratorFacade --> ComputerBuilder : builds custom selection
    PCConfiguratorFacade ..> PCResponse : returns result
    PCConfiguratorFacade ..> BuildDiagnostics : returns diagnostic data
    ComponentFactoryProvider --> ComponentFactory : supplies
    ComponentFactory <|.. IntelNvidiaComponentFactory
    ComponentFactory <|.. AMDComponentFactory
    IntelNvidiaComponentFactory ..> ComponentCatalog : resolves models
    AMDComponentFactory ..> ComponentCatalog : resolves models
    BuildDiagnosticsService --> ComponentFactoryProvider : validates platform
    BuildDiagnosticsService --> ComponentCatalog : resolves catalog metadata
    BuildDiagnosticsService --> CompatibilityChecker : applies checks
    BuildDiagnosticsService --> PowerRequirementCalculator : calculates power
    ComputerBuilder <|.. GamingComputerBuilder
    GamingComputerBuilder --> ComponentFactory : creates selected components
    ComputerOrderFactory --> ComputerBuilder : assembles through Builder
    ComputerBuilder ..> Computer : constructs

    BridgeReportController --> PCReportFacade : delegates export
    PCReportFacade --> PCConfiguratorFacade : obtains validated selection
    PCReportFacade --> ComputerReport : selects report refinement
    ComputerReport <|-- SummarySpecsReport
    ComputerReport <|-- FullDiagnosticReport
    ComputerReport --> ReportExporter : delegates output format
    ReportExporter <|.. HtmlReportExporter
    ReportExporter <|.. JsonReportExporter
```

The stress-test demonstration is intentionally a different workflow. Its `StressTestFacadeController` uses `PCConfiguratorFacade` for validated construction, then `SystemStressTestFacade` coordinates the simulated benchmark, rendering, and thermal subsystems.
