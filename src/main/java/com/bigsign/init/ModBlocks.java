package com.bigsign.init;

import com.bigsign.BigSignMod;
import com.bigsign.block.BigSignBlock;
import com.bigsign.block.BigWallSignBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BigSignMod.MODID);

    /** 1.21.1 用 registerBlock(String, Function&lt;Properties, Block&gt;)，属性由工厂回调定制。 */
    public static final DeferredBlock<BigSignBlock> BIG_SIGN =
            BLOCKS.registerBlock("big_sign", p -> new BigSignBlock(customize(p), WoodType.OAK));

    public static final DeferredBlock<BigWallSignBlock> BIG_WALL_SIGN =
            BLOCKS.registerBlock("big_wall_sign", p -> new BigWallSignBlock(customize(p), WoodType.OAK));

    private static BlockBehaviour.Properties customize(BlockBehaviour.Properties p) {
        return p.noCollission()
                .strength(1.0F)
                .noOcclusion()
                .sound(SoundType.WOOD);
    }
}
