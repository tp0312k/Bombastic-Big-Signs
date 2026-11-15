package com.bigsign;

import com.bigsign.client.ClientSetup;
import com.bigsign.init.ModBlockEntities;
import com.bigsign.init.ModBlocks;
import com.bigsign.init.ModItems;
import com.bigsign.network.BigSignNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(BigSignMod.MODID)
public class BigSignMod {

    public static final String MODID = "bigsign";

    public BigSignMod(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);

        modBus.addListener(BigSignNetwork::onRegisterPayloads);

        modBus.addListener(this::clientSetup);
        modBus.addListener(this::addCreative);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(ClientSetup::init);
    }

    private void addCreative(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.BIG_SIGN.get());
        }
    }
}
