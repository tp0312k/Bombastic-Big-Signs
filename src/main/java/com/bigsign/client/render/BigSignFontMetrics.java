package com.bigsign.client.render;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 自定义字体的运行时度量表。
 *
 * 书法/手写字体每个字符在字面框内的墨迹位置差异极大（出格、下沉、偏移），
 * 用离线统计的中位数常量定位会导致具体字符跑偏。本类在渲染时用 LWJGL 的
 * stb_truetype（与 Minecraft 字体管线完全相同的库与公式，见
 * TrueTypeGlyphProvider 字节码）读取每个字符的真实墨迹框：
 *
 *   pointScale = stbtt_ScaleForPixelHeight(info, size*oversample)
 *   iy0..iy1   = stbtt_GetGlyphBitmapBoxSubpixel(..., shiftX*OS, shiftY*OS)
 *   bearingX   = (lsb*pointScale + ix0 + shiftX*OS) / oversample
 *   bearingY   = (ascent*pointScale + iy0 + shiftY*OS) / oversample
 *   渲染墨迹框  = [x+bearingX, x+bearingX+w/OS] × [y+bearingY-3, y+bearingY-3+h/OS]
 *
 * 据此把每个字符的墨迹中心精确对齐到目标视觉位置。
 * TTF 缺字（子集未收录）时返回 null，调用方回退到默认字体定位
 * （此时 MC 也会逐字回退到 unifont 渲染，两者恰好一致）。
 */
public final class BigSignFontMetrics {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 必须与 font/*.json 中 ttf provider 的 size/oversample/shift 完全一致。 */
    private static final float SIZE = 11.0F;
    private static final float OVERSAMPLE = 4.0F;
    private static final float SHIFT_Y_OS = 1.0F * OVERSAMPLE;

    private record LoadedFont(STBTTFontinfo info, ByteBuffer data, float pointScale, float ascentPx) {
    }

    private static final Map<String, LoadedFont> LOADED = new HashMap<>();
    private static boolean loadFailed = false;

    private BigSignFontMetrics() {
    }

    /**
     * 字符的真实墨迹框（font px 单位，绘制点下方为正）：
     * left/right 为水平墨迹边界，top/bottom 为垂直墨迹边界（即 BakedGlyph 渲染四边形）。
     */
    public record GlyphBox(float left, float right, float top, float bottom) {
    }

    /**
     * 取指定字体中某字符的墨迹框；字体加载失败或 TTF 缺字时返回 null。
     */
    public static GlyphBox getGlyph(String fontId, int codepoint) {
        LoadedFont font = load(fontId);
        if (font == null) {
            return null;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int glyphIndex = STBTruetype.stbtt_FindGlyphIndex(font.info(), codepoint);
            if (glyphIndex == 0) {
                return null;
            }
            var advance = stack.mallocInt(1);
            var lsb = stack.mallocInt(1);
            STBTruetype.stbtt_GetGlyphHMetrics(font.info(), glyphIndex, advance, lsb);
            var ix0 = stack.mallocInt(1);
            var iy0 = stack.mallocInt(1);
            var ix1 = stack.mallocInt(1);
            var iy1 = stack.mallocInt(1);
            STBTruetype.stbtt_GetGlyphBitmapBoxSubpixel(font.info(), glyphIndex,
                    font.pointScale(), font.pointScale(), 0.0F, SHIFT_Y_OS, ix0, iy0, ix1, iy1);
            int width = ix1.get(0) - ix0.get(0);
            int height = iy1.get(0) - iy0.get(0);
            if (width <= 0 || height <= 0) {
                return null; // 空白字符
            }
            float bearingX = (lsb.get(0) * font.pointScale() + ix0.get(0)) / OVERSAMPLE;
            float bearingY = (font.ascentPx() + iy0.get(0) + SHIFT_Y_OS) / OVERSAMPLE;
            float left = bearingX;
            float right = left + width / OVERSAMPLE;
            float top = bearingY - 3.0F;
            float bottom = top + height / OVERSAMPLE;
            return new GlyphBox(left, right, top, bottom);
        }
    }

    private static synchronized LoadedFont load(String fontId) {
        LoadedFont font = LOADED.get(fontId);
        if (font != null) {
            return font;
        }
        if (loadFailed) {
            return null;
        }
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager()
                    .getResource(new ResourceLocation("bigsign", "font/" + fontId + ".ttf"));
            byte[] bytes;
            try (InputStream is = res.orElseThrow(() -> new IllegalStateException("font ttf not found")).open()) {
                bytes = is.readAllBytes();
            }
            // 直接内存缓冲，stbtt_InitFont 只保存指针，必须静态持有防止 GC
            ByteBuffer data = (ByteBuffer) BufferUtils.createByteBuffer(bytes.length).put(bytes).flip();
            STBTTFontinfo info = STBTTFontinfo.create();
            if (!STBTruetype.stbtt_InitFont(info, data)) {
                throw new IllegalStateException("stbtt_InitFont failed for " + fontId);
            }
            float pointScale = STBTruetype.stbtt_ScaleForPixelHeight(info, SIZE * OVERSAMPLE);
            float ascentPx;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var ascent = stack.mallocInt(1);
                var descent = stack.mallocInt(1);
                var lineGap = stack.mallocInt(1);
                STBTruetype.stbtt_GetFontVMetrics(info, ascent, descent, lineGap);
                ascentPx = ascent.get(0) * pointScale;
            }
            font = new LoadedFont(info, data, pointScale, ascentPx);
            LOADED.put(fontId, font);
            return font;
        } catch (Exception e) {
            loadFailed = true;
            LOGGER.warn("bigsign: failed to load font metrics for '{}', custom fonts fall back to default placement", fontId, e);
            return null;
        }
    }
}
