package com.aitu.sdp.assignment4.facade;

import com.aitu.sdp.assignment2.GPU;
import org.springframework.stereotype.Component;

/**
 * Simulates scene rendering and shader workloads for a graphics processor.
 * This subsystem owns graphical performance measurement and has no facade dependency.
 */
@Component
public class GPURenderTester {
    private static final int SCENE_FRAMES = 120;
    private static final int SHADER_OPERATIONS_PER_FRAME = 2_000;

    public double runRenderTest(GPU gpu) {
        if (gpu == null) {
            throw new IllegalArgumentException("GPU cannot be null");
        }
        if (gpu.getModel() == null || gpu.getModel().isBlank() || gpu.getTdpWatts() <= 0) {
            throw new IllegalArgumentException("GPU must have a model and positive TDP");
        }

        long startTime = System.nanoTime();
        double shaderResult = 0;
        for (int frame = 0; frame < SCENE_FRAMES; frame++) {
            for (int operation = 0; operation < SHADER_OPERATIONS_PER_FRAME; operation++) {
                double fragment = frame + operation * 0.01;
                shaderResult += Math.sqrt(fragment + 1) * Math.cos(fragment);
            }
        }
        long elapsedNanos = Math.max(1, System.nanoTime() - startTime);
        double renderedOperations = (double) SCENE_FRAMES * SHADER_OPERATIONS_PER_FRAME;
        return renderedOperations * 1_000_000_000d / elapsedNanos
                * (1 + gpu.getTdpWatts() / 1000d)
                * (1 + Math.abs(shaderResult % 1) / 1000);
    }
}
