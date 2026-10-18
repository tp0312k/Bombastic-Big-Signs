package com.bigsign.client;

import com.bigsign.BigSignMod;
import com.bigsign.block.BigSignBlock;
import com.bigsign.block.BigWallSignBlock;
import com.bigsign.block.entity.BigSignBlockEntity;
import com.bigsign.client.screen.BigSignEditScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BigSignMod.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {

    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getEntity().level();
        if (!level.isClientSide()) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        BlockState state = level.getBlockState(event.getPos());
        boolean isBigSign = state.getBlock() instanceof BigSignBlock
                || state.getBlock() instanceof BigWallSignBlock;
        if (!isBigSign) {
            return;
        }

        ItemStack stack = event.getItemStack();
        Item held = stack.getItem();
        boolean isInteractItem = held instanceof DyeItem
                || held == Items.GLOW_INK_SAC
                || held == Items.INK_SAC
                || stack.is(Items.HONEYCOMB)
                || held instanceof BlockItem;

        if (isInteractItem) {
            // Dye / glow ink / ink sac / wax are handled server-side, block items place normally.
            return;
        }

        event.setCanceled(true);

        if (level.getBlockEntity(event.getPos()) instanceof BigSignBlockEntity sign) {
            Minecraft.getInstance().setScreen(new BigSignEditScreen(event.getPos(), sign.getCharacter()));
        }
    }
}