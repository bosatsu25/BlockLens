package dev.blocklens.gametest;

import com.mojang.blaze3d.systems.RenderSystem;

/** Reads the initialized device rather than a requested setting. */
final class ExternalBackendProbe {
    private ExternalBackendProbe() { }

    static ExternalCompatibilityOracle.Backend read() {
        var device = RenderSystem.getDevice();
        return new ExternalCompatibilityOracle.Backend(
                device.getBackendName(), device.getRenderer(), device.getVersion());
    }
}
