package com.bigsign.block;

import com.bigsign.block.entity.BigSignBlockEntity;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

final class BigSignInteractions {

    private BigSignInteractions() {
    }

    /**
     * 手持物品右键（1.21 的 useItemOn）。染料/荧光墨囊/墨囊/蜂蜜脾与原版告示牌行为一致。
     * 非交互物品返回 PASS_TO_DEFAULT_BLOCK_INTERACTION，继续物品默认行为（如放置方块）。
     */
    static ItemInteractionResult applyWithItem(BlockEntity blockEntity, Level level, Player player, ItemStack stack) {
        if (!(blockEntity instanceof BigSignBlockEntity sign)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Item held = stack.getItem();

        if (held instanceof DyeItem dye) {
            if (!level.isClientSide) {
                sign.updateText(t -> t.setColor(dye.getDyeColor()), true);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (held == Items.GLOW_INK_SAC) {
            if (!level.isClientSide) {
                sign.updateText(t -> t.setHasGlowingText(true), true);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (held == Items.INK_SAC) {
            if (!level.isClientSide) {
                sign.updateText(t -> t.setHasGlowingText(false), true);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (held == Items.HONEYCOMB) {
            if (!level.isClientSide && !sign.isWaxed()) {
                // 复刻原版告示牌的打蜡封印逻辑
                sign.setWaxed(true);
                level.levelEvent(1038, sign.getBlockPos(), 0);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
