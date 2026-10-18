package com.aitu.sdp.assignment2;

/**
 * Shared builder implementation.
 *
 * <p>Clean Code principles: Single Responsibility and DRY. Common fluent
 * operations and validation live in one place; concrete builders only define
 * their configuration defaults.</p>
 */
abstract class AbstractComputerBuilder implements ComputerBuilder {
    private final ComponentFactory componentFactory;
    private CPU cpu;
    private GPU gpu;
    private String ram;
    private Storage storage;
    private PowerSupply powerSupply;
    private Motherboard motherboard;
    private CoolingSystem coolingSystem;
    private boolean hasRGB;

    protected AbstractComputerBuilder(ComponentFactory componentFactory, PerformanceTier tier) {
        if (componentFactory == null) {
            throw new IllegalArgumentException("Component factory cannot be null");
        }
        if (tier == null) {
            throw new IllegalArgumentException("Performance tier cannot be null");
        }
        this.componentFactory = componentFactory;
        cpu(componentFactory.createCPU(tier));
        gpu(componentFactory.createGPU(tier));
        motherboard(componentFactory.createMotherboard(tier));
        setDefaults(componentFactory, tier);
    }

    private void setDefaults(ComponentFactory componentFactory, PerformanceTier tier) {
        this.ram = tier == PerformanceTier.HIGH ? "32GB DDR5" : "16GB DDR4";
        this.storage = componentFactory.storage(tier);
        this.powerSupply = componentFactory.powerSupply(tier);
        this.coolingSystem = componentFactory.coolingSystem(tier);
        this.hasRGB = tier == PerformanceTier.HIGH;
    }

    @Override public ComputerBuilder cpu(String cpu) {
        return cpu(componentFactory.createCPU(requireText(cpu, "CPU")));
    }
    @Override public ComputerBuilder cpu(CPU cpu) {
        requireComponent(cpu, "CPU");
        this.cpu = cpu;
        return this;
    }
    @Override public ComputerBuilder gpu(String gpu) {
        return gpu(componentFactory.createGPU(requireText(gpu, "GPU")));
    }
    @Override public ComputerBuilder gpu(GPU gpu) {
        requireComponent(gpu, "GPU");
        this.gpu = gpu;
        return this;
    }
    @Override public ComputerBuilder ram(String ram) { this.ram = ram; return this; }
    @Override public ComputerBuilder storage(String storage) {
        return storage(new Storage(requireText(storage, "Storage")));
    }
    @Override public ComputerBuilder storage(Storage storage) {
        requireComponent(storage, "Storage");
        this.storage = storage;
        return this;
    }
    @Override public ComputerBuilder powerSupply(String powerSupply) {
        return powerSupply(componentFactory.createPowerSupply(requireText(powerSupply, "Power supply")));
    }
    @Override public ComputerBuilder powerSupply(PowerSupply powerSupply) {
        requireComponent(powerSupply, "Power supply");
        this.powerSupply = powerSupply;
        return this;
    }
    @Override public ComputerBuilder motherboard(String motherboard) {
        return motherboard(componentFactory.createMotherboard(requireText(motherboard, "Motherboard")));
    }
    @Override public ComputerBuilder motherboard(Motherboard motherboard) {
        requireComponent(motherboard, "Motherboard");
        this.motherboard = motherboard;
        return this;
    }
    @Override public ComputerBuilder coolingSystem(String coolingSystem) {
        return coolingSystem(new CoolingSystem(requireText(coolingSystem, "Cooling system")));
    }
    @Override public ComputerBuilder coolingSystem(CoolingSystem coolingSystem) {
        requireComponent(coolingSystem, "Cooling system");
        this.coolingSystem = coolingSystem;
        return this;
    }
    @Override public ComputerBuilder withRGB(boolean hasRGB) { this.hasRGB = hasRGB; return this; }

    @Override
    public final Computer build() {
        validateRequiredFields();
        validateSocketCompatibility();
        validatePowerCompatibility();
        return new Computer(cpu, gpu, ram, storage, powerSupply, motherboard, coolingSystem, hasRGB);
    }

    /** Clean Code principle: Fail-Fast and Validated Construction. */
    private void validateRequiredFields() {
        requireComponent(cpu, "CPU");
        requireComponent(gpu, "GPU");
        validate(ram, "RAM");
        requireComponent(storage, "Storage");
        requireComponent(powerSupply, "Power supply");
        requireComponent(motherboard, "Motherboard");
        requireComponent(coolingSystem, "Cooling system");
    }

    private void validate(String value, String component) {
        if (isBlank(value)) {
            throw new IllegalStateException(component + " is required to build a computer.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validatePowerCompatibility() {
        int selectedPower = powerSupply.wattage();
        int requiredPower = PowerRequirementCalculator.requiredWatts(
                cpu.getTdpWatts(), gpu.getTdpWatts());
        if (requiredPower > selectedPower) {
            throw new IncompatibleHardwareException("Power Supply (" + selectedPower + "W) is insufficient for "
                    + cpu.getModel() + " + " + gpu.getModel() + ". Minimum required: " + requiredPower + "W.");
        }
    }

    private void validateSocketCompatibility() {
        if (!cpu.getSocket().equals(motherboard.getSocket())) {
            throw new IncompatibleHardwareException("Socket mismatch: " + cpu.getModel()
                    + " requires " + cpu.getSocket() + ", but "
                    + motherboard.getModel() + " provides "
                    + motherboard.getSocket() + ".");
        }
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be null or blank");
        }
        return value.trim();
    }

    private void requireComponent(Object component, String name) {
        if (component == null) {
            throw new IllegalArgumentException(name + " component cannot be null");
        }
    }
}
