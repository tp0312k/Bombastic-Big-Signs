package com.bigsign.block.entity;

import com.bigsign.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.function.UnaryOperator;

public class BigSignBlockEntity extends SignBlockEntity {

    public BigSignBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIG_SIGN.get(), pos, state);
    }

    public String getCharacter() {
        return getFrontText().getMessage(0, false).getString();
    }

    public void setCharacter(String character) {
        Component ch = Component.literal(character == null ? "" : character);
        updateText(t -> t.setMessage(0, ch)
                        .setMessage(1, Component.empty())
                        .setMessage(2, Component.empty())
                        .setMessage(3, Component.empty()),
                true);
    }

    /**
     * 原版 updateText 只改字段不通知客户端（原版由 updateSignText 的调用方负责同步）。
     * 自定义编辑流程不经过原版 updateSignText，因此在这里统一补上同步，
     * 文字编辑与染料/荧光墨囊变色都会走这里。
     */
    @Override
    public boolean updateText(UnaryOperator<SignText> operator, boolean isFrontText) {
        boolean changed = super.updateText(operator, isFrontText);
        if (changed && this.level != null && !this.level.isClientSide) {
            this.setChanged();
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
        return changed;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return super.getRenderBoundingBox().inflate(0.5);
    }
}