package dev.covenant.client;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import dev.covenant.rpg.skills.ElementalGolem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Four approved appearances share one body/core animation set. Selection is captured in render state. */
public final class ElementalGolemRenderer extends GeoEntityRenderer<ElementalGolem, LivingEntityRenderState> {
    private static final DataTicket<ElementalGolem.Form> FORM = DataTicket.create("covenant:golem_form", ElementalGolem.Form.class);
    private static final Identifier BASE = id("entity/elemental_golem");
    private static final Identifier[] MODELS = {BASE, id("entity/elemental_golem_fire"), id("entity/elemental_golem_ice"), id("entity/elemental_golem_lightning")};
    private static final Identifier[] TEXTURES = {id("textures/entity/elemental_golem.png"), id("textures/entity/elemental_golem_fire.png"),
            id("textures/entity/elemental_golem_ice.png"), id("textures/entity/elemental_golem_lightning.png")};
    public ElementalGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<ElementalGolem>(id("elemental_golem")) {
            @Override public Identifier getModelResource(GeoRenderState state) { return MODELS[state.getOrDefaultGeckolibData(FORM, ElementalGolem.Form.ARCANE).ordinal()]; }
            @Override public Identifier getTextureResource(GeoRenderState state) { return TEXTURES[state.getOrDefaultGeckolibData(FORM, ElementalGolem.Form.ARCANE).ordinal()]; }
        });
        this.shadowRadius = .7f;
    }
    @Override public void addRenderData(ElementalGolem golem, Void related, LivingEntityRenderState state, float partialTick) {
        ((GeoRenderState) state).addGeckolibData(FORM, golem.form());
    }
    @Override protected float getDeathMaxRotation(GeoRenderState state) { return 0; }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("covenant", path); }
}
