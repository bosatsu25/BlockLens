package dev.blocklens.core.ui;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.ComfortOptions;
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

    public void setComfortOptions(ComfortOptions options) {
        config = config.withComfortOptions(options);
    }

    public boolean changed() {
        return config.enabledMask() != baseline.enabledMask()
                || !config.comfortOptions().equals(baseline.comfortOptions());
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
