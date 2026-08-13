package com.bettercontent.arcanechunkloaders.client;

import com.bettercontent.arcanechunkloaders.AnchorRegistries;
import com.bettercontent.arcanechunkloaders.ArcaneChunkLoadersMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ArcaneChunkLoadersMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AnchorClient {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AnchorRegistries.ARCANE_ANCHOR.get(), AnchorRenderer::new);
        event.registerBlockEntityRenderer(AnchorRegistries.KINETIC_ANCHOR.get(), AnchorRenderer::new);
    }

    private AnchorClient() {}
}
