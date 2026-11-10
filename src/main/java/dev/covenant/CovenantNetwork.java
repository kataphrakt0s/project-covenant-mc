package dev.covenant;

import net.neoforged.fml.ModList;

/** Required native payload negotiation rejects different mod releases before play begins. */
public final class CovenantNetwork {
    private CovenantNetwork() {}

    /** Keep the release version visible in NeoForge's mismatch screen, alongside each wire format revision. */
    public static String version(String protocol) {
        return ModList.get().getModContainerById(Covenant.MOD_ID).orElseThrow()
                .getModInfo().getVersion() + " (" + protocol + ")";
    }
}
