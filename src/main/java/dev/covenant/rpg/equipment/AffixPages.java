package dev.covenant.rpg.equipment;

import dev.covenant.rpg.RpgData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Trusted logical-server API; callers must mark a containing inventory changed after mutation.
 * No detached page bank exists. Native equipment attributes read the component and refresh on
 * equipment changes, including in-place component changes, without mutating permanent profiles.
 */
public final class AffixPages {
    private AffixPages() {}

    public static Optional<AffixPage> get(ItemStack stack) {
        return Optional.ofNullable(stack.get(RpgData.AFFIX_PAGE.get()));
    }

    /** Creates a new identity only when first attaching. Rejects stacked, incompatible, or paged items. */
    public static AffixPage attach(ServerLevel level, ItemStack stack, EquipmentCategory category,
            List<RpgModifier> modifiers, List<Affix> affixes) {
        requireServerThread(level);
        boolean supported = category.supports(stack) || ItemizationData.get(level.getServer()).participation().stream()
                .anyMatch(rule -> rule.category().equals(category) && rule.selector().matches(stack, category));
        if (stack.getCount() != 1 || stack.has(RpgData.AFFIX_PAGE.get()) || !supported) {
            throw new IllegalArgumentException("Affix Pages require one compatible, unpaged item");
        }
        AffixPage page = new AffixPage(UUID.randomUUID(), category, modifiers, affixes, PageProgression.NONE);
        // Setting this on the stack (not the item default) also supports naturally stackable modded gear.
        // A split(1) consumes the sole source item and moves the complete component to the result.
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        stack.set(RpgData.AFFIX_PAGE.get(), page);
        return page;
    }

    /** Replaces RPG values while preserving identity and category; also reasserts the singleton rule. */
    public static void update(ServerLevel level, ItemStack stack, List<RpgModifier> modifiers,
            List<Affix> affixes, PageProgression progression) {
        requireServerThread(level);
        AffixPage previous = get(stack).orElseThrow(() -> new IllegalArgumentException("Item has no Affix Page"));
        if (stack.getCount() != 1) {
            throw new IllegalArgumentException("Affix Page equipment must contain exactly one item");
        }
        AffixPage next = new AffixPage(previous.identity(), previous.category(), modifiers, affixes, progression);
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        stack.set(RpgData.AFFIX_PAGE.get(), next);
    }

    /**
     * Removes the entire page in place. Preserves every other component, including the singleton
     * limit: attachment did not retain its prior value, so restoring it would require guessing.
     * The caller must notify the containing inventory; native equipment updates remove its bonuses.
     */
    public static boolean remove(ServerLevel level, ItemStack stack) {
        requireServerThread(level);
        return stack.remove(RpgData.AFFIX_PAGE.get()) != null;
    }

    private static void requireServerThread(ServerLevel level) {
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("Affix Page changes require the logical server thread");
        }
    }
}
