package com.bigsign.init;

import com.bigsign.BigSignMod;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BigSignMod.MODID);

    public static final RegistryObject<Item> BIG_SIGN = ITEMS.register("big_sign",
            () -> new StandingAndWallBlockItem(
                    ModBlocks.BIG_SIGN.get(),
                    ModBlocks.BIG_WALL_SIGN.get(),
                    new Item.Properties().stacksTo(16),
                    Direction.DOWN));
}