package dev.covenant.rpg;

import dev.covenant.Covenant;
import dev.covenant.rpg.equipment.AffixPage;
import dev.covenant.rpg.skills.SkillState;
import dev.covenant.rpg.skills.SkillLoadout;
import dev.covenant.rpg.skills.SkillRuntime;
import dev.covenant.rpg.skills.BleedState;
import dev.covenant.rpg.skills.FrostSlows;
import dev.covenant.rpg.skills.ProjectileCombat;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Stable saved-data IDs. Changing these IDs or field meanings requires a migration. */
public final class RpgData {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Covenant.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Covenant.MOD_ID);
    /** Server-only native arrow payload; does not depend on the temporary cosmetic trail. */
    public static final Supplier<AttachmentType<dev.covenant.rpg.skills.StalkerArrows.Shot>> STALKER_SHOT = ATTACHMENTS.register(
            "stalker_shot", () -> AttachmentType.builder(() -> dev.covenant.rpg.skills.StalkerArrows.Shot.EMPTY)
                    .serialize(dev.covenant.rpg.skills.StalkerArrows.Shot.CODEC.fieldOf("shot")).build());
    /** Target-owned, saved Multishot attempts, pruned lazily on the next fan hit and cleared on death. */
    public static final Supplier<AttachmentType<dev.covenant.rpg.skills.StalkerArrows.Hits>> MULTISHOT_HITS = ATTACHMENTS.register(
            "multishot_hits", () -> AttachmentType.builder(() -> dev.covenant.rpg.skills.StalkerArrows.Hits.EMPTY)
                    .serialize(dev.covenant.rpg.skills.StalkerArrows.Hits.CODEC.fieldOf("hits")).build());
    /** Chunk-local placement exclusions; mutable changes explicitly mark the owning chunk unsaved. */
    public static final Supplier<AttachmentType<dev.covenant.rpg.progression.HarvestChunkState.PlacedOres>> PLACED_ORES = ATTACHMENTS.register(
            "placed_ores", () -> AttachmentType.builder(dev.covenant.rpg.progression.HarvestChunkState.PlacedOres::new)
                    .serialize(dev.covenant.rpg.progression.HarvestChunkState.PlacedOres.CODEC.fieldOf("positions")).build());
    /** Server-only, unsaved evidence from a bounded tree recognition request; invalidated by block edits. */
    public static final Supplier<AttachmentType<dev.covenant.rpg.progression.HarvestChunkState.Trees>> HARVEST_TREES = ATTACHMENTS.register(
            "harvest_trees", () -> AttachmentType.builder(dev.covenant.rpg.progression.HarvestChunkState.Trees::new).build());
    public static final Supplier<AttachmentType<dev.covenant.rpg.progression.CombatProgression.State>> COMBAT_PROGRESSION = ATTACHMENTS.register(
            "combat_progression", () -> AttachmentType.builder(dev.covenant.rpg.progression.CombatProgression.State::new).build());

    public static final Supplier<AttachmentType<CharacterProfile>> PROFILE = ATTACHMENTS.register(
            "character_profile", () -> AttachmentType.builder(() -> CharacterProfile.UNINITIALIZED)
                    .serialize(CharacterProfile.MAP_CODEC)
                    .copyOnDeath()
                    .sync((holder, recipient) -> holder == recipient, CharacterProfile.STREAM_CODEC)
                    .build());
    public static final Supplier<DataComponentType<AffixPage>> AFFIX_PAGE = COMPONENTS.registerComponentType(
            "affix_page", builder -> builder.persistent(AffixPage.CODEC)
                    .networkSynchronized(AffixPage.STREAM_CODEC));
    public static final Supplier<AttachmentType<SkillState>> SKILLS = ATTACHMENTS.register(
            "skill_state", () -> AttachmentType.builder(() -> SkillState.EMPTY)
                    .serialize(SkillState.CODEC.fieldOf("skills")).copyOnDeath()
                    .sync((holder, recipient) -> holder == recipient, SkillState.STREAM_CODEC).build());
    public static final Supplier<AttachmentType<SkillRuntime>> SKILL_RUNTIME = ATTACHMENTS.register(
            "skill_runtime", () -> AttachmentType.builder(SkillRuntime::new).build());
    /** Sole owner-saved companion snapshot; runtime reference is not serialized and death never copies a pet. */
    public static final Supplier<AttachmentType<dev.covenant.rpg.skills.HunterPets.State>> HUNTER_PET = ATTACHMENTS.register(
            "hunters_pet", () -> AttachmentType.builder(dev.covenant.rpg.skills.HunterPets.State::new)
                    .serialize(dev.covenant.rpg.skills.HunterPets.State.CODEC.fieldOf("companion")).build());
    /** Owner-saved native golem snapshot; runtime entity is unsaved and never copied on death. */
    public static final Supplier<AttachmentType<dev.covenant.rpg.skills.ElementalGolems.State>> ELEMENTAL_GOLEM = ATTACHMENTS.register(
            "elemental_golem", () -> AttachmentType.builder(dev.covenant.rpg.skills.ElementalGolems.State::new)
                    .serialize(dev.covenant.rpg.skills.ElementalGolems.State.CODEC.fieldOf("companion")).build());
    /** Unsaved owner-only elemental charge/reserve view; authority remains in SkillRuntime. */
    public static final Supplier<AttachmentType<dev.covenant.rpg.skills.WeaverDisplay>> WEAVER_DISPLAY = ATTACHMENTS.register(
            "weaver_display", () -> AttachmentType.builder(() -> dev.covenant.rpg.skills.WeaverDisplay.EMPTY)
                    .sync((holder, recipient) -> holder == recipient, dev.covenant.rpg.skills.WeaverDisplay.STREAM_CODEC).build());
    public static final Supplier<AttachmentType<dev.covenant.rpg.progression.ProfessionState>> PROFESSIONS = ATTACHMENTS.register(
            "profession_progression", () -> AttachmentType.builder(() -> dev.covenant.rpg.progression.ProfessionState.INITIAL)
                    .serialize(dev.covenant.rpg.progression.ProfessionState.CODEC.fieldOf("professions")).copyOnDeath()
                    .sync((holder, recipient) -> holder == recipient, dev.covenant.rpg.progression.ProfessionState.STREAM_CODEC).build());
    public static final Supplier<AttachmentType<dev.covenant.rpg.progression.ProgressionView>> PROGRESSION_VIEW = ATTACHMENTS.register(
            "progression_view", () -> AttachmentType.builder(() -> dev.covenant.rpg.progression.ProgressionView.EMPTY)
                    .sync((holder, recipient) -> holder == recipient, dev.covenant.rpg.progression.ProgressionView.STREAM_CODEC).build());
    /** Server-only saved discovery ledger and one-time claims, copied on death and End return. */
    public static final Supplier<AttachmentType<dev.covenant.rpg.progression.ExplorationState>> EXPLORATION = ATTACHMENTS.register(
            "exploration", () -> AttachmentType.builder(() -> dev.covenant.rpg.progression.ExplorationState.EMPTY)
                    .serialize(dev.covenant.rpg.progression.ExplorationState.CODEC.fieldOf("exploration")).copyOnDeath().build());
    public static final Supplier<AttachmentType<dev.covenant.rpg.progression.Exploration.Cursor>> EXPLORATION_CURSOR = ATTACHMENTS.register(
            "exploration_cursor", () -> AttachmentType.builder(dev.covenant.rpg.progression.Exploration.Cursor::new).build());
    public static final Supplier<AttachmentType<dev.covenant.rpg.progression.ExplorationView>> EXPLORATION_VIEW = ATTACHMENTS.register(
            "exploration_view", () -> AttachmentType.builder(() -> dev.covenant.rpg.progression.ExplorationView.EMPTY)
                    .sync((holder, recipient) -> holder == recipient, dev.covenant.rpg.progression.ExplorationView.STREAM_CODEC).build());
    /** A single hook pays at most once; disposable and never written to disk. */
    public static final Supplier<AttachmentType<Boolean>> FISHING_REWARDED = ATTACHMENTS.register(
            "fishing_rewarded", () -> AttachmentType.builder(() -> false).build());
    public static final Supplier<AttachmentType<dev.covenant.rpg.progression.ProfessionView>> PROFESSION_VIEW = ATTACHMENTS.register(
            "profession_view", () -> AttachmentType.builder(() -> dev.covenant.rpg.progression.ProfessionView.EMPTY)
                    .sync((holder, recipient) -> holder == recipient, dev.covenant.rpg.progression.ProfessionView.STREAM_CODEC).build());
    /** Two owner-only combo rows, updated only at transitions; no saved/copy state. */
    public static final Supplier<AttachmentType<dev.covenant.rpg.skills.ComboDisplay>> COMBO_DISPLAY = ATTACHMENTS.register(
            "combo_display", () -> AttachmentType.builder(() -> dev.covenant.rpg.skills.ComboDisplay.EMPTY)
                    .sync((holder, recipient) -> holder == recipient, dev.covenant.rpg.skills.ComboDisplay.STREAM_CODEC).build());
    /** Unsaved owner-only buff icons/timers, derived from authoritative grants and runtime transitions. */
    public static final Supplier<AttachmentType<dev.covenant.rpg.skills.BuffDisplay>> BUFF_DISPLAY = ATTACHMENTS.register(
            "buff_display", () -> AttachmentType.builder(() -> dev.covenant.rpg.skills.BuffDisplay.EMPTY)
                    .sync((holder, recipient) -> holder == recipient, dev.covenant.rpg.skills.BuffDisplay.STREAM_CODEC).build());
    /** Tracking-synced execution flag for input/control hooks; owner trajectory uses start/stop payloads. */
    public static final Supplier<AttachmentType<Boolean>> COMBO_MOVING = ATTACHMENTS.register(
            "combo_moving", () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());
    /** Active Frost patch references; never saved, synced, or copied to a replacement entity. */
    public static final Supplier<AttachmentType<FrostSlows.Sources>> FROST_SOURCES = ATTACHMENTS.register(
            "frost_sources", () -> AttachmentType.builder(FrostSlows.Sources::new).build());
    /** Target-owned saved wounds; relative timers pause offline, and death never copies them. */
    public static final Supplier<AttachmentType<BleedState>> BLEED = ATTACHMENTS.register(
            "bleed", () -> AttachmentType.builder(BleedState::new)
                    .serialize(BleedState.CODEC.fieldOf("bleed")).build());
    /** Transient, tracking-synced arrow visual marker; never serialized or copied to player saves. */
    public static final Supplier<AttachmentType<Boolean>> SKILL_SHOT = ATTACHMENTS.register(
            "skill_shot", () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());
    /** Unsaved tracking-synced arrow style: 0 Piercing/default, 1 Aimed, 2 Arcane, 3 Multishot. No combat authority. */
    public static final Supplier<AttachmentType<Integer>> SKILL_SHOT_STYLE = ATTACHMENTS.register(
            "skill_shot_style", () -> AttachmentType.builder(() -> 0).sync(ByteBufCodecs.VAR_INT).build());
    /** Cosmetic bow-draw deadline in level ticks, tracking-synced but never saved. */
    public static final Supplier<AttachmentType<Long>> BOW_DRAW_UNTIL = ATTACHMENTS.register(
            "bow_draw_until", () -> AttachmentType.builder(() -> 0L).sync(ByteBufCodecs.VAR_LONG).build());
    /** Cosmetic draw length in ticks; transient and tracking-synced, including scaled Aimed Shot casts. */
    public static final Supplier<AttachmentType<Long>> BOW_DRAW_DURATION = ATTACHMENTS.register(
            "bow_draw_duration", () -> AttachmentType.builder(() -> 6L).sync(ByteBufCodecs.VAR_LONG).build());
    public static final Supplier<AttachmentType<SkillLoadout>> SKILL_LOADOUT = ATTACHMENTS.register(
            "skill_loadout", () -> AttachmentType.builder(() -> SkillLoadout.EMPTY)
                    .serialize(SkillLoadout.CODEC.fieldOf("loadout")).copyOnDeath()
                    .sync((holder, recipient) -> holder == recipient, SkillLoadout.STREAM_CODEC).build());

    /** Temporary source/recipient ownership; no saved or copied derived buffs. */
    public static final Supplier<AttachmentType<dev.covenant.rpg.skills.Auras.State>> AURAS = ATTACHMENTS.register(
            "auras", () -> AttachmentType.builder(dev.covenant.rpg.skills.Auras.State::new).build());
    public static final Supplier<AttachmentType<dev.covenant.rpg.skills.AuraDamage.Combat>> AURA_COMBAT = ATTACHMENTS.register(
            "aura_combat", () -> AttachmentType.builder(dev.covenant.rpg.skills.AuraDamage.Combat::new).build());
    /** Only changes on freeze transitions, so client movement can use the authoritative state. */
    public static final Supplier<AttachmentType<Boolean>> AURA_FROZEN = ATTACHMENTS.register(
            "aura_frozen", () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());
    /** Separate owner-only shield capacity; never masquerades as native absorption. */
    public static final Supplier<AttachmentType<Float>> FORTITUDE_CAPACITY = ATTACHMENTS.register(
            "fortitude_capacity", () -> AttachmentType.builder(() -> 0f)
                    .sync((holder, recipient) -> holder == recipient, ByteBufCodecs.FLOAT).build());
    /** Additive save support at the existing ID; loaded native arrow damage must not be multiplied again. */
    public static final Supplier<AttachmentType<Double>> AURA_PROJECTILE_POWER = ATTACHMENTS.register(
            "aura_projectile_power", () -> AttachmentType.builder(() -> 1d)
                    .serialize(ProjectileCombat.POWER_CODEC.fieldOf("power")).build());
    /** Captured Enchanting damage survives projectile unload/reload; never synced to clients. */
    public static final Supplier<AttachmentType<Float>> ENCHANTED_SHOT = ATTACHMENTS.register(
            "enchanted_shot", () -> AttachmentType.builder(() -> 0f)
                    .serialize(ProjectileCombat.ENCHANTMENT_CODEC.fieldOf("damage")).build());
    /** New optional saved projectile metadata; missing legacy data has ordinary-shot defaults. */
    public static final Supplier<AttachmentType<ProjectileCombat>> PROJECTILE_COMBAT = ATTACHMENTS.register(
            "projectile_combat", () -> AttachmentType.builder(ProjectileCombat::new)
                    .serialize(ProjectileCombat.CODEC.fieldOf("combat")).build());

    private RpgData() {}

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        COMPONENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(RpgData::onLogin);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // Lazily creating the default is safe after loading existing saved attachments.
        // NeoForge also sends initial state on login, respawn, and dimension changes.
        event.getEntity().getData(PROFILE);
        event.getEntity().getData(PROFESSIONS);
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            dev.covenant.rpg.progression.Progression.refreshView(player);
        }
    }
}
