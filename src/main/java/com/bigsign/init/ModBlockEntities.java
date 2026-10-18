package com.bigsign.init;

import com.bigsign.BigSignMod;
import com.bigsign.block.entity.BigSignBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BigSignMod.MODID);

    public static final RegistryObject<BlockEntityType<BigSignBlockEntity>> BIG_SIGN =
            BLOCK_ENTITIES.register("big_sign",
                    () -> BlockEntityType.Builder.of(
                            BigSignBlockEntity::new,
                            ModBlocks.BIG_SIGN.get(),
                            ModBlocks.BIG_WALL_SIGN.get()).build(null));
}