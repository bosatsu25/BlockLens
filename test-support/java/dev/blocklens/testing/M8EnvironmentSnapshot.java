package dev.blocklens.testing;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.Objects;

/** Fixed, test-only environment fields; never retains VM arguments or arbitrary platform text. */
public record M8EnvironmentSnapshot(boolean escapeAnalysisEnabled, int javaFeature, int javaInterim,
        int javaUpdate, int javaPatch, OsFamily osFamily, int availableProcessors, Compilation compilation) {
    public enum OsFamily { WINDOWS, LINUX, MACOS, OTHER }

    public record Compilation(boolean tieredCompilation, int tieredStopAtLevel,
            boolean backgroundCompilation, long reservedCodeCacheBytes) {
        public Compilation {
            if (tieredStopAtLevel < 0 || tieredStopAtLevel > 4
                    || reservedCodeCacheBytes < 1 || reservedCodeCacheBytes > 2_147_483_648L) {
                throw new IllegalArgumentException("Invalid numeric M8 compilation value");
            }
        }
    }

    public M8EnvironmentSnapshot {
        Objects.requireNonNull(osFamily);
        Objects.requireNonNull(compilation);
        if (javaFeature < 1 || javaInterim < 0 || javaUpdate < 0 || javaPatch < 0 || availableProcessors < 1) {
            throw new IllegalArgumentException("Invalid numeric M8 environment value");
        }
    }

    public static M8EnvironmentSnapshot capture() {
        HotSpotDiagnosticMXBean bean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        if (bean == null) throw new IllegalStateException("M8 requires the HotSpot diagnostic MXBean");
        Compilation compilation = new Compilation(booleanOption(bean, "TieredCompilation"),
                Math.toIntExact(numericOption(bean, "TieredStopAtLevel")),
                booleanOption(bean, "BackgroundCompilation"), numericOption(bean, "ReservedCodeCacheSize"));
        Runtime.Version version = Runtime.version();
        return new M8EnvironmentSnapshot(booleanOption(bean, "DoEscapeAnalysis"), version.feature(), version.interim(),
                version.update(), version.patch(), osFamily(System.getProperty("os.name", "")),
                Runtime.getRuntime().availableProcessors(), compilation);
    }

    private static boolean booleanOption(HotSpotDiagnosticMXBean bean, String name) {
        String value = bean.getVMOption(name).getValue();
        if (!value.equals("true") && !value.equals("false")) {
            throw new IllegalStateException("M8 VM option is not boolean: " + name);
        }
        return Boolean.parseBoolean(value);
    }

    private static long numericOption(HotSpotDiagnosticMXBean bean, String name) {
        try {
            return Long.parseLong(bean.getVMOption(name).getValue());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("M8 VM option is not an integer: " + name);
        }
    }

    public static OsFamily osFamily(String name) {
        if (name == null) return OsFamily.OTHER;
        if (name.startsWith("Windows")) return OsFamily.WINDOWS;
        if (name.equals("Linux")) return OsFamily.LINUX;
        if (name.startsWith("Mac") || name.equals("Darwin")) return OsFamily.MACOS;
        return OsFamily.OTHER;
    }

    public String manifest() {
        return "vmDoEscapeAnalysis=" + escapeAnalysisEnabled + "\n"
                + "javaFeature=" + javaFeature + "\njavaInterim=" + javaInterim
                + "\njavaUpdate=" + javaUpdate + "\njavaPatch=" + javaPatch + "\n"
                + "osFamily=" + osFamily.name().toLowerCase(Locale.ROOT) + "\n"
                + "availableProcessors=" + availableProcessors + "\n"
                + "vmTieredCompilation=" + compilation.tieredCompilation() + "\n"
                + "vmTieredStopAtLevel=" + compilation.tieredStopAtLevel() + "\n"
                + "vmBackgroundCompilation=" + compilation.backgroundCompilation() + "\n"
                + "vmReservedCodeCacheBytes=" + compilation.reservedCodeCacheBytes() + "\n";
    }
}
