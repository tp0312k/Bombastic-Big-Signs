package com.bigsign;

import com.bigsign.client.ClientSetup;
import com.bigsign.init.ModBlockEntities;
import com.bigsign.init.ModBlocks;
import com.bigsign.init.ModItems;
import com.bigsign.network.BigSignNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BigSignMod.MODID)
public class BigSignMod {

    public static final String MODID = "bigsign";

    public BigSignMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);

        BigSignNetwork.register();

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