package com.bigsign.init;

import com.bigsign.BigSignMod;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BigSignMod.MODID);

    /** 1.21.1 构造器顺序：(站立方块, 墙面方块, Properties, 附着方向)；注册用 registerItem。 */
    public static final DeferredItem<StandingAndWallBlockItem> BIG_SIGN = ITEMS.registerItem("big_sign",
            p -> new StandingAndWallBlockItem(
                    ModBlocks.BIG_SIGN.get(),
                    ModBlocks.BIG_WALL_SIGN.get(),
                    p.stacksTo(16),
                    Direction.DOWN));
}
