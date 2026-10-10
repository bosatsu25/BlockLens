package dev.blocklens.gametest;

import com.mojang.blaze3d.systems.RenderSystem;

/** Compiled separately against 26.2 and 26.3; both expose actual device information. */
final class ExternalBackendProbe {
    private ExternalBackendProbe() { }

    static ExternalCompatibilityOracle.Backend read() {
        var device = RenderSystem.getDevice().getDeviceInfo();
        return new ExternalCompatibilityOracle.Backend(
                device.backendName(), device.name(), device.driverInfo());
    }
}
