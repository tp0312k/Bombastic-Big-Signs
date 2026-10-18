package com.bigsign.block.entity;

import com.bigsign.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.function.UnaryOperator;

public class BigSignBlockEntity extends SignBlockEntity {

    /** 板面颜色：-1 = 原始贴图（保留边框），0..15 = DyeColor ordinal（纯色，无边框）。 */
    public static final int BOARD_ORIGINAL = -1;

    private int boardColor = BOARD_ORIGINAL;
    /** 字体 id（bigsign 内置字体资源，见 assets/bigsign/font/*.json，空串 = 默认）。 */
    private String fontId = "";

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

    public int getBoardColor() {
        return this.boardColor;
    }

    public void setBoardColor(int boardColor) {
        if (this.boardColor != boardColor) {
            this.boardColor = boardColor;
            this.markStyleChanged();
        }
    }

    public String getFontId() {
        return this.fontId;
    }

    public void setFontId(String fontId) {
        String id = fontId == null ? "" : fontId;
        if (!this.fontId.equals(id)) {
            this.fontId = id;
            this.markStyleChanged();
        }
    }

    /** 样式变更后同步到客户端（仅服务端调用生效）。 */
    private void markStyleChanged() {
        if (this.level != null && !this.level.isClientSide) {
            this.setChanged();
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
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
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("BigSignBoardColor", this.boardColor);
        tag.putString("BigSignFontId", this.fontId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.boardColor = tag.contains("BigSignBoardColor") ? tag.getInt("BigSignBoardColor") : BOARD_ORIGINAL;
        this.fontId = tag.contains("BigSignFontId") ? tag.getString("BigSignFontId") : "";
    }

    @Override
    public CompoundTag getUpdateTag() {
        // saveWithoutMetadata 走 saveAdditional，自动带上样式字段
        return this.saveWithoutMetadata();
    }

    @Override
    public AABB getRenderBoundingBox() {
        return super.getRenderBoundingBox().inflate(0.5);
    }
}
