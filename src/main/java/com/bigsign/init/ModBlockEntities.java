package com.bigsign.init;

import com.bigsign.BigSignMod;
import com.bigsign.block.entity.BigSignBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, BigSignMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BigSignBlockEntity>> BIG_SIGN =
            BLOCK_ENTITIES.register("big_sign",
                    () -> BlockEntityType.Builder.of(
                            BigSignBlockEntity::new,
                            ModBlocks.BIG_SIGN.get(),
                            ModBlocks.BIG_WALL_SIGN.get()).build(null));
}
