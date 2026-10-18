package com.bigsign.client;

import com.bigsign.client.render.BigSignRenderer;
import com.bigsign.init.ModBlockEntities;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class ClientSetup {

    private ClientSetup() {
    }

    public static void init() {
        BlockEntityRenderers.register(ModBlockEntities.BIG_SIGN.get(), BigSignRenderer::new);
    }
}