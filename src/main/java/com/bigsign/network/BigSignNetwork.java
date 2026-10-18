package com.bigsign.network;

import com.bigsign.BigSignMod;
import com.bigsign.block.entity.BigSignBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * 自定义网络通道。
 *
 * 背景：Trae 原实现用客户端直开的编辑界面 + 原版 ServerboundSignUpdatePacket，
 * 但 1.20.1 服务端 SignBlockEntity.updateSignText 只接受
 * playerWhoMayEdit == 玩家 UUID 的更新，而该权限只能由服务端
 * player.openTextEdit() 授予，导致每次打字都被拒绝
 * （日志：Player xxx just tried to change non-editable sign）。
 * 这里改用自定义包，服务端自行校验距离后写入 BlockEntity。
 */
public final class BigSignNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BigSignMod.MODID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private BigSignNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, SetTextPacket.class,
                SetTextPacket::encode, SetTextPacket::decode, SetTextPacket::handle);
    }

    /** 客户端 → 服务端：设置大字牌正面文字（单个字符）。 */
    public static class SetTextPacket {

        private final BlockPos pos;
        private final String text;

        public SetTextPacket(BlockPos pos, String text) {
            this.pos = pos;
            this.text = text == null ? "" : text;
        }

        public static void encode(SetTextPacket packet, FriendlyByteBuf buf) {
            buf.writeBlockPos(packet.pos);
            buf.writeUtf(packet.text, 16);
        }

        public static SetTextPacket decode(FriendlyByteBuf buf) {
            return new SetTextPacket(buf.readBlockPos(), buf.readUtf(16));
        }

        public static void handle(SetTextPacket packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) {
                    return;
                }
                ServerLevel level = player.serverLevel();
                BlockPos pos = packet.pos;
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
                    String text = packet.text;
                    if (text.length() > 1) {
                        text = text.substring(0, 1);
                    }
                    sign.setCharacter(text);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
