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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
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
     * 墙面牌：牌板同样由本渲染器绘制（需要按方块实体的颜色染色）。
     *
     * 贴图走独立纹理直绑（RenderType.entitySolid(贴图路径)），与原版告示牌
     * （textures/entity/signs/*.png 独立贴图）同机制。不要改回方块图集
     * （Material/TextureAtlasSprite/wrap）——在 Embeddium/Iris 环境下图集采样
     * 会塌缩成单像素颜色（表现为整个牌面变成边框色或填充色）。
     */
    private static final ResourceLocation BOARD_TEXTURE =
            new ResourceLocation("bigsign", "textures/block/big_sign.png");
    private static final ResourceLocation BOARD_BACK_TEXTURE =
            new ResourceLocation("bigsign", "textures/block/big_sign_back.png");
    private static final ResourceLocation BOARD_SIDE_TEXTURE =
            new ResourceLocation("bigsign", "textures/block/big_sign_side.png");
    private static final ResourceLocation BOARD_WHITE_TEXTURE =
            new ResourceLocation("bigsign", "textures/block/big_sign_white.png");

    private static final float STANDING_TEXT_OFFSET = 0.51F;
    private static final float WALL_TEXT_OFFSET = -0.365F;

    /**
     * mod 内置自定义字体（assets/bigsign/font/&lt;id&gt;.json + 同名 .ttf）。
     * 缩放按离线校准的中位数墨迹高度确定（保持各字体相对字重，详见 .workbuddy/fontwork/calib.py）；
     * 位置则由 BigSignFontMetrics 在运行时按每个字符的真实墨迹框精确居中——
     * 书法字体逐字墨迹位置差异极大，固定偏移常量会导致具体字跑偏。
     */
    private record FontMetrics(float wideScale, float narrowScale) {
    }

    private static final java.util.Map<String, FontMetrics> CUSTOM_FONTS = java.util.Map.of(
            "kaiti", new FontMetrics(0.08433F, 0.07111F),
            "mangcao", new FontMetrics(0.08433F, 0.05818F),
            "naipao", new FontMetrics(0.12144F, 0.09600F)
    );

    /** 默认 unifont 视觉基准（世界单位，牌面中心 y=0.5）：墨迹高 0.759/0.48，墨迹中心一律对齐牌面正中。 */
    private static final float WIDE_TARGET_H = 0.759F;
    private static final float WIDE_TARGET_C = 0.5F;
    private static final float NARROW_TARGET_H = 0.48F;
    private static final float NARROW_TARGET_C = 0.5F;

    private final Font font;

    public BigSignRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(BigSignBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
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

        // 板：站立牌与文字同角度；墙面牌用模型旋转角（板随方块实体颜色染色，纯色时无边框）
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(boardYaw));
        renderBoard(pose, buffer, packedLight, packedOverlay, entity.getBoardColor());
        pose.popPose();

        String character = entity.getCharacter();
        if (character == null || character.isBlank()) {
            return;
        }

        DyeColor dye = entity.getFrontText().getColor();
        int color = dye != null ? (0xFF000000 | (dye.getTextColor() & 0xFFFFFF)) : 0xFF000000;

        // 字体：空串/未知 id 走默认字体（Font 端对未知字体 id 自动回退，不会崩溃）
        Style style = Style.EMPTY;
        FontMetrics metrics = null;
        String fontId = entity.getFontId();
        if (fontId != null) {
            metrics = CUSTOM_FONTS.get(fontId);
            if (metrics != null) {
                style = style.withFont(new ResourceLocation("bigsign", fontId));
            }
        }

        FormattedCharSequence sequence = FormattedCharSequence.forward(character, style);
        boolean wide = character.codePointAt(0) > 0xFF;

        // 位置：自定义字体按每字符真实墨迹框运行时精确居中（缺字回退默认定位，与
        // MC 逐字回退 unifont 渲染一致）；默认 unifont 用历次目测校准结果。
        float scale;
        float x;
        float y;
        BigSignFontMetrics.GlyphBox box = metrics != null
                ? BigSignFontMetrics.getGlyph(fontId, character.codePointAt(0)) : null;
        if (box != null) {
            scale = wide ? metrics.wideScale() : metrics.narrowScale();
            float targetC = wide ? WIDE_TARGET_C : NARROW_TARGET_C;
            // x：墨迹水平居中后整体右移 1 个纹理像素（0.96/16 = 0.06 格）
            x = -(box.left() + box.right()) / 2.0F + 0.06F / scale;
            // y：world_dy = -scale * font_y（pose.y 取负），墨迹中心对齐目标高度
            y = (0.5F - targetC) / scale - (box.top() + box.bottom()) / 2.0F;
        } else {
            // 默认 unifont：全角字形（汉字等）16x16 放大 15%；ASCII 取 FACE/14。
            // y 使墨迹中心落在牌面正中：unifont 位图字形墨迹顶=绘制点（bearingY 默认 3）。
            scale = wide ? (FACE / 16.0F) * 1.15F : (FACE / 14.0F);
            x = -this.font.width(sequence) / 2.0F + 0.06F / scale;
            y = wide ? -(WIDE_TARGET_H / 2.0F) / scale : -(NARROW_TARGET_H / 2.0F) / scale;
        }

        // 文字
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(0.0, 0.0, textOffset);
        pose.scale(scale, -scale, 1.0F);

        if (entity.getFrontText().hasGlowingText()) {
            font.drawInBatch8xOutline(sequence, x, y, color, getOutlineColor(dye), pose.last().pose(), buffer, FULLBRIGHT);
        } else {
            font.drawInBatch(sequence, x, y, color, false, pose.last().pose(), buffer,
                    Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
        }

        pose.popPose();
    }

    /**
     * 荧光文字的描边颜色，复刻原版 SignRenderer.getDarkColor：
     * 黑字（含默认）荧光时用奶白 0xF0ECCC 描边（否则黑边吃掉黑字），
     * 其余颜色按 RGB×0.4 调暗成同色调深色，比纯黑柔和。
     */
    private static int getOutlineColor(DyeColor dye) {
        if (dye == null || dye == DyeColor.BLACK) {
            return 0xFFF0ECCC;
        }
        int rgb = dye.getTextColor();
        int r = (int) ((rgb >> 16 & 0xFF) * 0.4);
        int g = (int) ((rgb >> 8 & 0xFF) * 0.4);
        int b = (int) ((rgb & 0xFF) * 0.4);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /**
     * 手动绘制牌板（一个 16x16x2px 的薄板，位于局部 z ∈ [0.375, 0.5]），
     * 姿态由调用方先旋转好。boardColor ≥ 0 时用纯白贴图 + 顶点色染成纯色（无边框）；
     * -1 时用原始贴图（保留边框）。
     */
    private void renderBoard(PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay, int boardColor) {
        float r = 1.0F, g = 1.0F, b = 1.0F;
        ResourceLocation front;
        ResourceLocation back;
        ResourceLocation side;
        if (boardColor >= 0) {
            front = BOARD_WHITE_TEXTURE;
            back = BOARD_WHITE_TEXTURE;
            side = BOARD_WHITE_TEXTURE;
            float[] tint = DyeColor.values()[boardColor].getTextureDiffuseColors();
            r = tint[0];
            g = tint[1];
            b = tint[2];
        } else {
            front = BOARD_TEXTURE;
            back = BOARD_BACK_TEXTURE;
            side = BOARD_SIDE_TEXTURE;
        }
        VertexConsumer frontVc = buffer.getBuffer(RenderType.entitySolid(front));
        VertexConsumer backVc = buffer.getBuffer(RenderType.entitySolid(back));
        VertexConsumer sideVc = buffer.getBuffer(RenderType.entitySolid(side));
        PoseStack.Pose p = pose.last();

        float x0 = -0.5F, x1 = 0.5F;
        float y0 = -0.5F, y1 = 0.5F;
        float z0 = 0.375F, z1 = 0.5F;
        float e = 2.0F / 16.0F; // 侧边条宽 2px

        // 正面（南，+z）→ big_sign.png
        quad(p, frontVc, packedLight, packedOverlay, r, g, b,
                x0, y0, z1, 0.0F, 1.0F,
                x1, y0, z1, 1.0F, 1.0F,
                x1, y1, z1, 1.0F, 0.0F,
                x0, y1, z1, 0.0F, 0.0F,
                0.0F, 0.0F, 1.0F);
        // 背面（北，-z）→ big_sign_back.png
        quad(p, backVc, packedLight, packedOverlay, r, g, b,
                x1, y0, z0, 0.0F, 1.0F,
                x0, y0, z0, 1.0F, 1.0F,
                x0, y1, z0, 1.0F, 0.0F,
                x1, y1, z0, 0.0F, 0.0F,
                0.0F, 0.0F, -1.0F);
        // 东侧（+x）→ big_sign_side.png 左侧 2px 竖条
        quad(p, sideVc, packedLight, packedOverlay, r, g, b,
                x1, y0, z1, 0.0F, 1.0F,
                x1, y0, z0, e, 1.0F,
                x1, y1, z0, e, 0.0F,
                x1, y1, z1, 0.0F, 0.0F,
                1.0F, 0.0F, 0.0F);
        // 西侧（-x）→ big_sign_side.png 左侧 2px 竖条
        quad(p, sideVc, packedLight, packedOverlay, r, g, b,
                x0, y0, z0, 0.0F, 1.0F,
                x0, y0, z1, e, 1.0F,
                x0, y1, z1, e, 0.0F,
                x0, y1, z0, 0.0F, 0.0F,
                -1.0F, 0.0F, 0.0F);
        // 顶（+y）→ big_sign_side.png 顶部 2px 横条
        quad(p, sideVc, packedLight, packedOverlay, r, g, b,
                x0, y1, z1, 0.0F, e,
                x1, y1, z1, 1.0F, e,
                x1, y1, z0, 1.0F, 0.0F,
                x0, y1, z0, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F);
        // 底（-y）→ big_sign_side.png 顶部 2px 横条
        quad(p, sideVc, packedLight, packedOverlay, r, g, b,
                x0, y0, z0, 0.0F, 0.0F,
                x1, y0, z0, 1.0F, 0.0F,
                x1, y0, z1, 1.0F, e,
                x0, y0, z1, 0.0F, e,
                0.0F, -1.0F, 0.0F);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vc, int light, int overlay,
                             float r, float g, float b,
                             float ax, float ay, float az, float au, float av,
                             float bx, float by, float bz, float bu, float bv,
                             float cx, float cy, float cz, float cu, float cv,
                             float dx, float dy, float dz, float du, float dv,
                             float nx, float ny, float nz) {
        vertex(pose, vc, light, overlay, r, g, b, ax, ay, az, au, av, nx, ny, nz);
        vertex(pose, vc, light, overlay, r, g, b, bx, by, bz, bu, bv, nx, ny, nz);
        vertex(pose, vc, light, overlay, r, g, b, cx, cy, cz, cu, cv, nx, ny, nz);
        vertex(pose, vc, light, overlay, r, g, b, dx, dy, dz, du, dv, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, int light, int overlay,
                               float r, float g, float b,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        vc.vertex(pose.pose(), x, y, z);
        vc.color(r, g, b, 1.0F);
        // 独立贴图，uv 直接用 0..1 全图坐标（无图集，无渗色，无 mipmap 干扰）
        vc.uv(u, v);
        vc.overlayCoords(overlay & 0xFFFF, (overlay >> 16) & 0xFFFF);
        vc.uv2(light & 0xFFFF, (light >> 16) & 0xFFFF);
        vc.normal(pose.normal(), nx, ny, nz);
        vc.endVertex();
    }
}
