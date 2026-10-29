package dev.blocklens.core.ui;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.ComfortOptions;
import dev.blocklens.core.analyzer.AnalyzerOptions;
import dev.blocklens.core.scene.SceneFilterOptions;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Local edits; a failed write never advances the saved baseline. */
public final class SettingsDraft {
    private BlockLensConfig baseline;
    private BlockLensConfig config;

    public SettingsDraft(BlockLensConfig baseline) {
        this.baseline = Objects.requireNonNull(baseline, "baseline");
        this.config = baseline;
    }

    public BlockLensConfig config() {
        return config;
    }

    public void toggle(CapabilityId capability) {
        config = config.withEnabled(capability, !config.isEnabled(capability));
    }

    /** NewGlowingOres vanilla ore subset; original CTM artwork is not copied. */
    public void enableOreGlowProfile() {
        for (CapabilityId capability : CapabilityId.values()) {
            if (capability.isResourceHighlight()
                    && capability != CapabilityId.OBSIDIAN
                    && capability != CapabilityId.CRYING_OBSIDIAN) {
                config = config.withEnabled(capability, true);
            }
        }
    }

    /** Disables every Resources control, including connected borders, without touching other settings. */
    public void disableResources() {
        for (CapabilityId capability : SettingsCatalog.capabilities(CapabilityId.Category.RESOURCE)) {
            config = config.withEnabled(capability, false);
        }
    }

    /** Enables every capability the source resource pack offers, across all categories; keeps the rest. */
    public void enableSourcePack() {
        for (CapabilityId capability : CapabilityId.values()) {
            if (capability.isSourcePackCapability()) {
                config = config.withEnabled(capability, true);
            }
        }
    }

    /** Enables both obsidian controls while preserving every other flag. */
    public void enableObsidianPair() {
        config = config.withEnabled(CapabilityId.OBSIDIAN, true)
                .withEnabled(CapabilityId.CRYING_OBSIDIAN, true);
    }

    public void setComfortOptions(ComfortOptions options) {
        config = config.withComfortOptions(options);
    }

    public void setAnalyzerOptions(AnalyzerOptions options) { config = config.withAnalyzerOptions(options); }

    public void setSceneFilterOptions(SceneFilterOptions options) { config = config.withSceneFilterOptions(options); }

    public boolean changed() {
        return !config.sameCapabilities(baseline)
                || !config.comfortOptions().equals(baseline.comfortOptions())
                || !config.analyzerOptions().equals(baseline.analyzerOptions())
                || !config.sceneFilterOptions().equals(baseline.sceneFilterOptions());
    }

    public void discard() {
        config = baseline;
    }

    /** Saves changed edits only; the caller installs the snapshot after this returns successfully. */
    public boolean save(Path directory) throws IOException {
        if (!changed()) {
            return false;
        }
        BlockLensConfigFiles.save(directory, config);
        baseline = config;
        return true;
    }
}
