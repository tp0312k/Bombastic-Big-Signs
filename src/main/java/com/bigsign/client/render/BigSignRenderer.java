package com.bigsign.client.render;

import com.bigsign.block.entity.BigSignBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BigSignRenderer implements BlockEntityRenderer<BigSignBlockEntity> {

    private static final int FULLBRIGHT = 0xF000F0;
    private static final float FACE = 0.96F;

    /**
     * 模型约定：牌板位于模型空间南缘（z=14..16px），文字面朝外。
     * 站立牌：牌板由本渲染器绘制（blockstate 的 y 旋转只支持 90° 倍数，无法表达 16 向旋转）。
     * 墙面牌：牌板由方块模型绘制（朝向只有 4 个，90° 步进够用），渲染器只画文字。
     */
    private static final ResourceLocation BOARD_TEXTURE =
            new ResourceLocation("bigsign", "block/big_sign");
    private static final ResourceLocation BOARD_BACK_TEXTURE =
            new ResourceLocation("bigsign", "block/big_sign_back");
    private static final ResourceLocation BOARD_SIDE_TEXTURE =
            new ResourceLocation("bigsign", "block/big_sign_side");
    private static final ResourceLocation BOARD_WHITE_TEXTURE =
            new ResourceLocation("bigsign", "block/big_sign_white");

    private static final float STANDING_TEXT_OFFSET = 0.51F;
    private static final float WALL_TEXT_OFFSET = -0.365F;

    private final Font font;

    public BigSignRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(BigSignBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        String character = entity.getCharacter();
        if (character == null || character.isBlank()) {
            return;
        }

        BlockState state = entity.getBlockState();
        boolean wall = state.getBlock() instanceof WallSignBlock;
        float yaw;
        float boardYaw;
        float textOffset;
        if (wall) {
            // 局部 +z 轴旋转到“朝外”方向
            Direction facing = state.getValue(WallSignBlock.FACING);
            yaw = -facing.toYRot();
            // 板面复刻原方块模型/blockstate 的旋转（与文字角度相差 180° 补偿），两者位置才能对齐
            boardYaw = -switch (facing) {
                case NORTH -> 0;
                case EAST -> 90;
                case SOUTH -> 180;
                default -> 270;
            };
            textOffset = WALL_TEXT_OFFSET;
        } else {
            // 局部 +z 轴旋转到牌子正面方向
            yaw = -(state.getValue(StandingSignBlock.ROTATION) * 22.5F);
            boardYaw = yaw;
            textOffset = STANDING_TEXT_OFFSET;
        }

        DyeColor dye = entity.getFrontText().getColor();
        int color = dye != null ? (0xFF000000 | (dye.getTextColor() & 0xFFFFFF)) : 0xFF000000;

        FormattedCharSequence sequence = FormattedCharSequence.forward(character, Style.EMPTY);
        boolean wide = character.codePointAt(0) > 0xFF;

        // unifont 全角字形（汉字等）为 16x16；放大 15% 后墨迹约占牌面 73%，为视觉主体。
        // ASCII 字形约 5x8（墨迹高 7px）：取 FACE/14 使 1 字体像素 ≈ 0.069 格，
        // 与汉字笔画粗细一致（墨迹高约牌面 48%），避免过粗盖住边框。
        float scale = wide ? (FACE / 16.0F) * 1.15F : (FACE / 14.0F);

        // 板：站立牌与文字同角度；墙面牌用模型旋转角（板随方块实体颜色染色，纯色时无边框）
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(boardYaw));
        renderBoard(pose, buffer, packedLight, entity.getBoardColor());
        pose.popPose();

        // 文字
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(0.0, 0.0, textOffset);
        pose.scale(scale, -scale, 1.0F);

        float x = -font.width(sequence) / 2.0F;
        // 用户要求中/英/数整体右移 1 个纹理像素（0.96/16 = 0.06 格）：
        // 0.06 / 0.069 ≈ 0.87 字体像素（两种字号的比例几乎相同，共用同一偏移）。
        x += 0.87F;
        // unifont 全角字形墨迹在 16px 单元格内顶部对齐（墨迹约为绘制点下方 +0.5..+11.5px，中心 ≈ +5.3px）。
        // 用户要求在居中基础上再下移约 2 个纹理像素（0.125 格）：0.5 - 0.125 ≈ 0.415 →
        // (0.5 - 0.415) / 0.069 ≈ 1.23 → 绘制点 y = 1.23 - 5.3 ≈ -4.1。
        // ASCII 墨迹在绘制点下方 0..7px（中心 ≈ +3.5px）；用户先后校准：下移 1px 后又上移 1px → 恢复严格居中：
        // 0.5 - 0.0686 * (y + 3.5) = 0.5 → y = -3.5。
        float y = wide ? -4.1F : -3.5F;

        if (entity.getFrontText().hasGlowingText()) {
            font.drawInBatch8xOutline(sequence, x, y, color, 0xFF000000, pose.last().pose(), buffer, FULLBRIGHT);
        } else {
            font.drawInBatch(sequence, x, y, color, false, pose.last().pose(), buffer,
                    Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
        }

        pose.popPose();
    }

    /**
     * 手动绘制牌板（一个 16x16x2px 的薄板，位于局部 z ∈ [0.375, 0.5]），
     * 姿态由调用方先旋转好。boardColor ≥ 0 时用纯白贴图 + 顶点色染成纯色（无边框）；
     * -1 时用原始贴图（保留边框）。
     */
    private void renderBoard(PoseStack pose, MultiBufferSource buffer, int packedLight, int boardColor) {
        float r = 1.0F, g = 1.0F, b = 1.0F;
        TextureAtlasSprite front;
        TextureAtlasSprite back;
        TextureAtlasSprite side;
        if (boardColor >= 0) {
            TextureAtlasSprite white = new Material(InventoryMenu.BLOCK_ATLAS, BOARD_WHITE_TEXTURE).sprite();
            front = white;
            back = white;
            side = white;
            float[] tint = DyeColor.values()[boardColor].getTextureDiffuseColors();
            r = tint[0];
            g = tint[1];
            b = tint[2];
        } else {
            front = new Material(InventoryMenu.BLOCK_ATLAS, BOARD_TEXTURE).sprite();
            back = new Material(InventoryMenu.BLOCK_ATLAS, BOARD_BACK_TEXTURE).sprite();
            side = new Material(InventoryMenu.BLOCK_ATLAS, BOARD_SIDE_TEXTURE).sprite();
        }
        VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
        PoseStack.Pose p = pose.last();

        float x0 = -0.5F, x1 = 0.5F;
        float y0 = -0.5F, y1 = 0.5F;
        float z0 = 0.375F, z1 = 0.5F;
        float e = 2.0F / 16.0F; // 侧边条宽 2px

        // 正面（南，+z）→ big_sign.png
        quad(p, vc, front, packedLight, r, g, b,
                x0, y0, z1, 0.0F, 1.0F,
                x1, y0, z1, 1.0F, 1.0F,
                x1, y1, z1, 1.0F, 0.0F,
                x0, y1, z1, 0.0F, 0.0F,
                0.0F, 0.0F, 1.0F);
        // 背面（北，-z）→ big_sign_back.png
        quad(p, vc, back, packedLight, r, g, b,
                x1, y0, z0, 0.0F, 1.0F,
                x0, y0, z0, 1.0F, 1.0F,
                x0, y1, z0, 1.0F, 0.0F,
                x1, y1, z0, 0.0F, 0.0F,
                0.0F, 0.0F, -1.0F);
        // 东侧（+x）→ big_sign_side.png 左侧 2px 竖条
        quad(p, vc, side, packedLight, r, g, b,
                x1, y0, z1, 0.0F, 1.0F,
                x1, y0, z0, e, 1.0F,
                x1, y1, z0, e, 0.0F,
                x1, y1, z1, 0.0F, 0.0F,
                1.0F, 0.0F, 0.0F);
        // 西侧（-x）→ big_sign_side.png 左侧 2px 竖条
        quad(p, vc, side, packedLight, r, g, b,
                x0, y0, z0, 0.0F, 1.0F,
                x0, y0, z1, e, 1.0F,
                x0, y1, z1, e, 0.0F,
                x0, y1, z0, 0.0F, 0.0F,
                -1.0F, 0.0F, 0.0F);
        // 顶（+y）→ big_sign_side.png 顶部 2px 横条
        quad(p, vc, side, packedLight, r, g, b,
                x0, y1, z1, 0.0F, e,
                x1, y1, z1, 1.0F, e,
                x1, y1, z0, 1.0F, 0.0F,
                x0, y1, z0, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F);
        // 底（-y）→ big_sign_side.png 顶部 2px 横条
        quad(p, vc, side, packedLight, r, g, b,
                x0, y0, z0, 0.0F, 0.0F,
                x1, y0, z0, 1.0F, 0.0F,
                x1, y0, z1, 1.0F, e,
                x0, y0, z1, 0.0F, e,
                0.0F, -1.0F, 0.0F);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vc, TextureAtlasSprite sprite, int light,
                             float r, float g, float b,
                             float ax, float ay, float az, float au, float av,
                             float bx, float by, float bz, float bu, float bv,
                             float cx, float cy, float cz, float cu, float cv,
                             float dx, float dy, float dz, float du, float dv,
                             float nx, float ny, float nz) {
        vertex(pose, vc, sprite, light, r, g, b, ax, ay, az, au, av, nx, ny, nz);
        vertex(pose, vc, sprite, light, r, g, b, bx, by, bz, bu, bv, nx, ny, nz);
        vertex(pose, vc, sprite, light, r, g, b, cx, cy, cz, cu, cv, nx, ny, nz);
        vertex(pose, vc, sprite, light, r, g, b, dx, dy, dz, du, dv, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, TextureAtlasSprite sprite, int light,
                               float r, float g, float b,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        vc.vertex(pose.pose(), x, y, z);
        vc.color(r, g, b, 1.0F);
        // 半像素内缩，避免小贴图在方块图集中 mipmap 时渗入相邻纹理（表现为边缘青色杂点）
        float inset = 0.5F / 16.0F;
        vc.uv(sprite.getU(inset + u * (1.0F - 2.0F * inset)), sprite.getV(inset + v * (1.0F - 2.0F * inset)));
        vc.uv2(light & 0xFFFF, (light >> 16) & 0xFFFF);
        vc.normal(pose.normal(), nx, ny, nz);
        vc.endVertex();
    }
}
