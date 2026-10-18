package com.bigsign.block;

import com.bigsign.block.entity.BigSignBlockEntity;
import net.minecraft.world.InteractionResult;
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

    static InteractionResult apply(BlockEntity blockEntity, Level level, Player player, ItemStack stack) {
        if (!(blockEntity instanceof BigSignBlockEntity sign)) {
            return InteractionResult.PASS;
        }

        Item held = stack.getItem();

        if (held instanceof DyeItem dye) {
            if (!level.isClientSide) {
                sign.updateText(t -> t.setColor(dye.getDyeColor()), true);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (held == Items.GLOW_INK_SAC) {
            if (!level.isClientSide) {
                sign.updateText(t -> t.setHasGlowingText(true), true);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (held == Items.INK_SAC) {
            if (!level.isClientSide) {
                sign.updateText(t -> t.setHasGlowingText(false), true);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }
}