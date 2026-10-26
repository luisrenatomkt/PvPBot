package com.pvpbot;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

/** Renderiza o bot como um jogador (modelo de jogador, skin Steve 64x64 propria do mod, armadura e item na mao). */
public class RenderPvPBot extends RenderBiped<EntityPvPBot> {
    private static final ResourceLocation SKIN = new ResourceLocation("pvpbot", "textures/entity/steve.png");

    public RenderPvPBot(RenderManager manager) {
        super(manager, new ModelPlayer(0.0F, false), 0.5F);
    }

    @Override
    public void doRender(EntityPvPBot entity, double x, double y, double z, float yaw, float partialTicks) {
        ((ModelBiped) this.mainModel).heldItemRight = entity.getHeldItem() != null ? 1 : 0;
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityPvPBot entity) {
        return SKIN;
    }
}
