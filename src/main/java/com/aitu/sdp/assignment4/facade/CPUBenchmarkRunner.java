package com.aitu.sdp.assignment4.facade;

import com.aitu.sdp.assignment2.CPU;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Runs parallel arithmetic workloads against a processor and returns operations per second.
 * This subsystem owns CPU benchmark execution and has no dependency on the facade.
 */
@Component
public class CPUBenchmarkRunner {
    private static final int ITERATIONS_PER_WORKER = 100_000;
    private static final int MAX_WORKERS = 4;

    public double runBenchmark(CPU cpu) {
        if (cpu == null) {
            throw new IllegalArgumentException("CPU cannot be null");
        }
        if (cpu.getModel() == null || cpu.getModel().isBlank() || cpu.getTdpWatts() <= 0) {
            throw new IllegalArgumentException("CPU must have a model and positive TDP");
        }

        int workerCount = Math.max(1, Math.min(
                Runtime.getRuntime().availableProcessors(), MAX_WORKERS));
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        List<Future<Double>> workloads = new ArrayList<>(workerCount);
        long startTime = System.nanoTime();
        try {
            for (int worker = 0; worker < workerCount; worker++) {
                final int workerIndex = worker;
                workloads.add(executor.submit(() -> calculateWorkload(workerIndex)));
            }

            double checksum = 0;
            for (Future<Double> workload : workloads) {
                checksum += workload.get();
            }
            long elapsedNanos = Math.max(1, System.nanoTime() - startTime);
            double operationsPerSecond = (double) workerCount * ITERATIONS_PER_WORKER
                    * 1_000_000_000d / elapsedNanos;
            double processorCapacityFactor = 1 + cpu.getTdpWatts() / 500d;
            return operationsPerSecond * processorCapacityFactor
                    * (1 + Math.abs(checksum % 1) / 1000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("CPU benchmark was interrupted", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("CPU benchmark workload failed", exception.getCause());
        } finally {
            executor.shutdownNow();
        }
    }

    private double calculateWorkload(int workerIndex) {
        double result = 0;
        for (int iteration = 1; iteration <= ITERATIONS_PER_WORKER; iteration++) {
            double value = iteration + workerIndex;
            result += Math.sqrt(value) * Math.sin(value);
        }
        return result;
    }
}
