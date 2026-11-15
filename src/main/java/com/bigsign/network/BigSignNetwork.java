package com.bigsign.network;

import com.bigsign.BigSignMod;
import com.bigsign.block.entity.BigSignBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 自定义网络载荷（NeoForge 1.21 的 CustomPacketPayload 体系）。
 *
 * 背景：Trae 原实现用客户端直开的编辑界面 + 原版 ServerboundSignUpdatePacket，
 * 但服务端 SignBlockEntity.updateSignText 只接受
 * playerWhoMayEdit == 玩家 UUID 的更新，而该权限只能由服务端
 * player.openTextEdit() 授予，导致每次打字都被拒绝
 * （日志：Player xxx just tried to change non-editable sign）。
 * 这里改用自定义包，服务端自行校验距离后写入 BlockEntity。
 */
public final class BigSignNetwork {

    private BigSignNetwork() {
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SetTextPayload.TYPE, SetTextPayload.STREAM_CODEC, BigSignNetwork::handleText);
        registrar.playToServer(SetStylePayload.TYPE, SetStylePayload.STREAM_CODEC, BigSignNetwork::handleStyle);
    }

    /** 客户端 → 服务端：设置大字牌正面文字（单个字符）。 */
    public record SetTextPayload(BlockPos pos, String text) implements CustomPacketPayload {

        public static final Type<SetTextPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(BigSignMod.MODID, "set_text"));

        public static final StreamCodec<ByteBuf, SetTextPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, SetTextPayload::pos,
                ByteBufCodecs.stringUtf8(16), SetTextPayload::text,
                SetTextPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 客户端 → 服务端：设置样式（文字颜色 / 板面颜色 / 字体）。整包发送完整状态。 */
    public record SetStylePayload(BlockPos pos, int textDyeIdx, int boardColor, String fontId)
            implements CustomPacketPayload {

        public static final Type<SetStylePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(BigSignMod.MODID, "set_style"));

        public static final StreamCodec<ByteBuf, SetStylePayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, SetStylePayload::pos,
                ByteBufCodecs.VAR_INT, SetStylePayload::textDyeIdx,
                ByteBufCodecs.VAR_INT, SetStylePayload::boardColor,
                ByteBufCodecs.stringUtf8(32), SetStylePayload::fontId,
                SetStylePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void handleText(SetTextPayload packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            ServerLevel level = player.serverLevel();
            BlockPos pos = packet.pos();
            if (!level.hasChunkAt(pos)) {
                return;
            }
            // 防滥用：仅允许 8 格内编辑
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                return;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BigSignBlockEntity sign) {
                // 原版规则：蜡封的告示牌不可编辑（创造模式豁免，与原版一致）。
                if (sign.isWaxed() && !player.getAbilities().instabuild) {
                    return;
                }
                // 服务端兜底：无论客户端发来什么，只取第一个字符（与编辑界面限制一致）。
                String text = packet.text() == null ? "" : packet.text();
                if (!text.isEmpty() && text.codePointCount(0, text.length()) > 1) {
                    text = new String(Character.toChars(text.codePointAt(0)));
                }
                sign.setCharacter(text);
            }
        });
    }

    private static void handleStyle(SetStylePayload packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            ServerLevel level = player.serverLevel();
            BlockPos pos = packet.pos();
            if (!level.hasChunkAt(pos)) {
                return;
            }
            // 防滥用：仅允许 8 格内编辑
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                return;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BigSignBlockEntity sign) {
                // 原版规则：蜡封的告示牌不可编辑（创造模式豁免，与原版一致）。
                if (sign.isWaxed() && !player.getAbilities().instabuild) {
                    return;
                }
                int dyeIdx = packet.textDyeIdx();
                if (dyeIdx >= 0 && dyeIdx < DyeColor.values().length) {
                    DyeColor color = DyeColor.values()[dyeIdx];
                    sign.updateText(t -> t.setColor(color), true);
                }
                int boardColor = packet.boardColor();
                if (boardColor >= -1 && boardColor < DyeColor.values().length) {
                    sign.setBoardColor(boardColor);
                }
                sign.setFontId(packet.fontId() == null ? "" : packet.fontId());
            }
        });
    }

    /** 客户端便捷发送。 */
    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
