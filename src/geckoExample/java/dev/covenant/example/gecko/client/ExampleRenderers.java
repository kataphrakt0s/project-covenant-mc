package dev.covenant.example.gecko.client;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.GeoItemRenderer;
import java.util.function.Consumer;
import dev.covenant.example.gecko.ExampleBlockEntity;
import dev.covenant.example.gecko.ExampleEntity;
import dev.covenant.example.gecko.ExampleItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Compile-only physical-client wiring. Call from the client mod entry point's
 * RegisterRenderers listener after promoting and registering a concrete asset.
 * These resource names match the disposable Blockbench gecko-smoke exports.
 */
public final class ExampleRenderers {
    private ExampleRenderers() {}

    public static void register(EntityRenderersEvent.RegisterRenderers event,
                                EntityType<ExampleEntity> entityType,
                                BlockEntityType<ExampleBlockEntity> blockEntityType) {
        event.registerEntityRenderer(entityType, context -> new GeoEntityRenderer<>(context,
                new DefaultedEntityGeoModel<>(id("gecko_probe_entity"))));
        event.registerBlockEntityRenderer(blockEntityType, context -> new GeoBlockRenderer<>(context,
                new DefaultedBlockGeoModel<>(id("gecko_probe_block"))));
    }

    /** Only called through GeoItem's physical-client guarded, memoized provider. */
    public static void createItemRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<ExampleItem> renderer;

            @Override
            public GeoItemRenderer<ExampleItem> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new GeoItemRenderer<>(new DefaultedItemGeoModel<>(id("gecko_probe_item")));
                }
                return renderer;
            }
        });
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("covenant", name);
    }
}
