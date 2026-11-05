package dev.covenant.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client controls policy; never changes server gameplay or other mods' mappings. */
public final class SkillClientConfig {
    public static final ModConfigSpec SPEC;
    /** Default true: clear vanilla Drop Item when bound to Q at startup; retain any other drop key. */
    public static final ModConfigSpec.BooleanValue UNBIND_DROP_ITEM;
    /** Default true: clear vanilla Swap Hands when bound to F at startup; retain any other swap key. */
    public static final ModConfigSpec.BooleanValue UNBIND_SWAP_HANDS;

    static {
        var builder = new ModConfigSpec.Builder();
        UNBIND_DROP_ITEM = builder.comment("Default: true. At client startup, unbind vanilla Drop Item if it uses Q,",
                "freeing Q and Shift+Q for skills. Other drop bindings are retained.",
                "Set false to keep Q for dropping items, then rebind skill slots in Controls.")
                .define("unbindDropItem", true);
        UNBIND_SWAP_HANDS = builder.comment("Default: true. At client startup, unbind vanilla Swap Hands if it uses F,",
                "freeing F and Shift+F for skills. Other swap bindings are retained.",
                "Set false to keep F for swapping hands, then rebind skill slots in Controls.")
                .define("unbindSwapHands", true);
        SPEC = builder.build();
    }

    private SkillClientConfig() {}
}
