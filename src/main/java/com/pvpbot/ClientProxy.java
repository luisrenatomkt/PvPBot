package com.pvpbot;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

public class ClientProxy extends CommonProxy {
    @Override
    public void registerRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityPvPBot.class, new IRenderFactory<EntityPvPBot>() {
            @Override
            public Render<? super EntityPvPBot> createRenderFor(RenderManager manager) {
                return new RenderPvPBot(manager);
            }
        });
    }
}
