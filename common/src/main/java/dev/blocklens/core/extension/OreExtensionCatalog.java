package dev.blocklens.core.extension;

import dev.blocklens.core.CapabilityId;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Bounded, deterministic input policy shared by the explicit API and synchronized ore tags. */
public final class OreExtensionCatalog {
    public static final int API_VERSION = 1;
    public static final int MAX_TARGETS = 256;
    public static final int MAX_ID_LENGTH = 128;
    public static final int MAX_STATES_PER_TARGET = 64;
    public static final int MAX_STATE_MODELS = 4096;
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    private final Map<String, CapabilityId> registrations = new TreeMap<>();
    private Map<String, CapabilityId> frozen;
    private boolean overflowed;

    public synchronized boolean register(int version, String blockId, String capabilityKey) {
        if (version != API_VERSION || capabilityKey == null || capabilityKey.length() > MAX_ID_LENGTH) {
            return false;
        }
        return add(blockId, CapabilityId.bySourceKey().get(capabilityKey));
    }

    public synchronized boolean add(String blockId, CapabilityId capability) {
        if (frozen != null || blockId == null || blockId.length() > MAX_ID_LENGTH
                || blockId.startsWith("minecraft:") || !ID.matcher(blockId).matches()
                || capability == null || capability.category() != CapabilityId.Category.RESOURCE) {
            return false;
        }
        if (registrations.containsKey(blockId)) {
            if (registrations.get(blockId) == capability) return true;
            registrations.put(blockId, null);
            return false;
        }
        if (registrations.size() >= MAX_TARGETS) {
            overflowed = true;
            return false;
        }
        registrations.put(blockId, capability);
        return true;
    }

    /** Null values are conflict tombstones: tag fallback must not reactivate an explicit conflict. */
    public synchronized Map<String, CapabilityId> freeze() {
        if (frozen == null) {
            frozen = Collections.unmodifiableMap(new TreeMap<>(registrations));
        }
        return frozen;
    }

    public synchronized boolean overflowed() {
        return overflowed;
    }

    /** Fixed material order: coal, copper, diamond, emerald, gold, iron, lapis, scrap, quartz, redstone. */
    public static CapabilityId taggedCapability(int material, boolean deepslate, boolean netherrack) {
        if (deepslate && netherrack) return null;
        return switch (material) {
            case 0 -> deepslate ? CapabilityId.DEEPSLATE_COAL_ORE : CapabilityId.COAL_ORE;
            case 1 -> deepslate ? CapabilityId.DEEPSLATE_COPPER_ORE : CapabilityId.COPPER_ORE;
            case 2 -> deepslate ? CapabilityId.DEEPSLATE_DIAMOND_ORE : CapabilityId.DIAMOND_ORE;
            case 3 -> deepslate ? CapabilityId.DEEPSLATE_EMERALD_ORE : CapabilityId.EMERALD_ORE;
            case 4 -> deepslate ? CapabilityId.DEEPSLATE_GOLD_ORE
                    : netherrack ? CapabilityId.NETHER_GOLD_ORE : CapabilityId.GOLD_ORE;
            case 5 -> deepslate ? CapabilityId.DEEPSLATE_IRON_ORE : CapabilityId.IRON_ORE;
            case 6 -> deepslate ? CapabilityId.DEEPSLATE_LAPIS_ORE : CapabilityId.LAPIS_ORE;
            case 7 -> CapabilityId.ANCIENT_DEBRIS;
            case 8 -> CapabilityId.NETHER_QUARTZ_ORE;
            case 9 -> deepslate ? CapabilityId.DEEPSLATE_REDSTONE_ORE : CapabilityId.REDSTONE_ORE;
            default -> null;
        };
    }
}
