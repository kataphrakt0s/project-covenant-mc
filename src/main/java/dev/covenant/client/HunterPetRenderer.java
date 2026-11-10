package dev.covenant.client;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import dev.covenant.rpg.skills.HunterPet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Physical-client renderer for the shipped original wolf atlas and five validated clips. */
public final class HunterPetRenderer extends GeoEntityRenderer<HunterPet, LivingEntityRenderState> {
    public HunterPetRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(Identifier.fromNamespaceAndPath("covenant", "original_wolf")));
        this.shadowRadius = .5f;
    }
    @Override protected float getDeathMaxRotation(GeoRenderState state) { return 0; }
}
