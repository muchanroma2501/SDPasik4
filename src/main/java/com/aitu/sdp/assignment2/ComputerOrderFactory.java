package com.aitu.sdp.assignment2;

/**
 * Factory Method creator for complete computer orders.
 *
 * <p>Clean Code principle: Polymorphism over Conditionals. The creator
 * delegates builder selection to subclasses and contains no type conditionals.</p>
 */
public abstract class ComputerOrderFactory {
    protected abstract ComputerBuilder createBuilder();

    public Computer orderComputer() {
        return orderComputer(null, null, null);
    }

    public Computer orderComputer(String ram, String storage, Boolean hasRGB) {
        return orderComputer(ram, storage, null, hasRGB);
    }

    public Computer orderComputer(String ram, String storage, String coolingSystem, Boolean hasRGB) {
        ComputerBuilder builder = createBuilder();
        if (ram != null) {
            builder.ram(ram);
        }
        if (storage != null) {
            builder.storage(storage);
        }
        if (coolingSystem != null) {
            builder.coolingSystem(coolingSystem);
        }
        if (hasRGB != null) {
            builder.withRGB(hasRGB);
        }
        return builder.build();
    }
}
