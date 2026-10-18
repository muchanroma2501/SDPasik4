package com.aitu.sdp.assignment2.controller;

/** Payload for a fully custom component selection. */
public class CustomPCRequest {
    private String ecosystem;
    private String cpu;
    private String gpu;
    private String motherboard;
    private String ram;
    private String storage;
    private String powerSupply;
    private String coolingSystem;
    private boolean hasRGB;

    public String getEcosystem() { return ecosystem; }
    public void setEcosystem(String ecosystem) { this.ecosystem = ecosystem; }
    public String getCpu() { return cpu; }
    public void setCpu(String cpu) { this.cpu = cpu; }
    public String getGpu() { return gpu; }
    public void setGpu(String gpu) { this.gpu = gpu; }
    public String getMotherboard() { return motherboard; }
    public void setMotherboard(String motherboard) { this.motherboard = motherboard; }
    public String getRam() { return ram; }
    public void setRam(String ram) { this.ram = ram; }
    public String getStorage() { return storage; }
    public void setStorage(String storage) { this.storage = storage; }
    public String getPowerSupply() { return powerSupply; }
    public void setPowerSupply(String powerSupply) { this.powerSupply = powerSupply; }
    public String getCoolingSystem() { return coolingSystem; }
    public void setCoolingSystem(String coolingSystem) { this.coolingSystem = coolingSystem; }
    public boolean isHasRGB() { return hasRGB; }
    public void setHasRGB(boolean hasRGB) { this.hasRGB = hasRGB; }
}
