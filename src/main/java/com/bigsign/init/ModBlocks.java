package com.bigsign.init;

import com.bigsign.BigSignMod;
import com.bigsign.block.BigSignBlock;
import com.bigsign.block.BigWallSignBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, BigSignMod.MODID);

    private static BlockBehaviour.Properties signProps() {
        return BlockBehaviour.Properties.of()
                .noCollission()
                .strength(1.0F)
                .noOcclusion()
                .sound(SoundType.WOOD);
    }

    public static final RegistryObject<Block> BIG_SIGN =
            BLOCKS.register("big_sign", () -> new BigSignBlock(signProps(), WoodType.OAK));

    public static final RegistryObject<Block> BIG_WALL_SIGN =
            BLOCKS.register("big_wall_sign", () -> new BigWallSignBlock(signProps(), WoodType.OAK));
}