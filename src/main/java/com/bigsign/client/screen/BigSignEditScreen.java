package com.bigsign.client.screen;

import com.bigsign.network.BigSignNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class BigSignEditScreen extends Screen {

    /** 下拉选项。 */
    private record Entry(String label, int swatch, int value) {
    }

    private static final String[] DYE_NAMES = {
            "白色", "橙色", "品红色", "淡蓝色", "黄色", "黄绿色", "粉红色", "灰色",
            "淡灰色", "青色", "紫色", "蓝色", "棕色", "绿色", "红色", "黑色"
    };
    private static final String[][] FONTS = {
            {"默认字体", ""},
            {"楷体", "kaiti"},
            {"行书", "mangcao"},
            {"奶泡体", "naipao"}
    };

    private static final int DROPDOWN_NONE = 0;
    private static final int DROPDOWN_TEXT = 1;
    private static final int DROPDOWN_BOARD = 2;
    private static final int DROPDOWN_FONT = 3;

    private final BlockPos pos;
    private final String existing;
    private EditBox input;
    private boolean submitted;

    private int curTextColor;
    private int curBoardColor;
    private String curFontId;

    private Button textColorBtn;
    private Button boardColorBtn;
    private Button fontBtn;
    private int openDropdown = DROPDOWN_NONE;
    // 面板几何（每帧 render 时计算，供点击判定）
    private int panelX, panelY, panelW, panelH;
    private int panelCols, panelRows;
    private int panelEntryCount;

    public BigSignEditScreen(BlockPos pos, String existing, int textDyeIdx, int boardColor, String fontId) {
        super(Component.translatable("bigsign.screen.title"));
        this.pos = pos;
        this.existing = existing == null ? "" : existing;
        this.curTextColor = textDyeIdx;
        this.curBoardColor = boardColor;
        this.curFontId = fontId == null ? "" : fontId;
        this.submitted = false;
    }

    @Override
    protected void init() {
        this.input = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 10, 200, 20,
                Component.translatable("bigsign.screen.title"));
        this.input.setMaxLength(1);
        this.input.setValue(this.existing.isEmpty() ? "" : this.existing.substring(0, 1));
        this.input.setFocused(true);
        this.setFocused(this.input);
        this.addRenderableWidget(this.input);

        int by = this.height / 2 + 14;
        this.textColorBtn = this.addRenderableWidget(Button.builder(textColorLabel(), b -> toggle(DROPDOWN_TEXT))
                .bounds(this.width / 2 - 100, by, 66, 16).build());
        this.boardColorBtn = this.addRenderableWidget(Button.builder(boardColorLabel(), b -> toggle(DROPDOWN_BOARD))
                .bounds(this.width / 2 - 32, by, 66, 16).build());
        this.fontBtn = this.addRenderableWidget(Button.builder(fontLabel(), b -> toggle(DROPDOWN_FONT))
                .bounds(this.width / 2 + 36, by, 66, 16).build());
    }

    private Component textColorLabel() {
        return Component.literal("字色:" + dyeName(this.curTextColor));
    }

    private Component boardColorLabel() {
        return Component.literal("版面:" + (this.curBoardColor < 0 ? "原始" : dyeName(this.curBoardColor)));
    }

    private Component fontLabel() {
        String name = "默认";
        for (String[] f : FONTS) {
            if (f[1].equals(this.curFontId)) {
                name = f[0];
            }
        }
        return Component.literal("字体:" + name);
    }

    private static String dyeName(int idx) {
        return DYE_NAMES[Math.floorMod(idx, DYE_NAMES.length)];
    }

    private void toggle(int which) {
        this.openDropdown = this.openDropdown == which ? DROPDOWN_NONE : which;
    }

    /** 构建当前下拉的选项列表。 */
    private List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        if (this.openDropdown == DROPDOWN_TEXT) {
            for (int i = 0; i < DyeColor.values().length; i++) {
                list.add(new Entry(DYE_NAMES[i], DyeColor.values()[i].getTextColor(), i));
            }
        } else if (this.openDropdown == DROPDOWN_BOARD) {
            list.add(new Entry("原始贴图", -1, -1));
            for (int i = 0; i < DyeColor.values().length; i++) {
                float[] c = DyeColor.values()[i].getTextureDiffuseColors();
                int rgb = 0xFF000000 | ((int) (c[0] * 255) << 16) | ((int) (c[1] * 255) << 8) | (int) (c[2] * 255);
                list.add(new Entry(DYE_NAMES[i], rgb, i));
            }
        } else if (this.openDropdown == DROPDOWN_FONT) {
            for (int i = 0; i < FONTS.length; i++) {
                list.add(new Entry(FONTS[i][0], 0xFF888888, i));
            }
        }
        return list;
    }

    /** 计算面板几何（render 与 mouseClicked 共用）。 */
    private void layoutPanel() {
        List<Entry> list = entries();
        this.panelEntryCount = list.size();
        this.panelCols = this.openDropdown == DROPDOWN_FONT ? 1 : 2;
        this.panelRows = (this.panelEntryCount + this.panelCols - 1) / this.panelCols;
        this.panelW = this.panelCols * 62 + 2;
        this.panelH = this.panelRows * 12 + 2;
        this.panelX = switch (this.openDropdown) {
            case DROPDOWN_TEXT -> this.width / 2 - 100;
            case DROPDOWN_BOARD -> this.width / 2 - 32;
            default -> this.width / 2 + 36;
        };
        this.panelY = this.height / 2 + 32;
        // 底部放不下就往上弹
        if (this.panelY + this.panelH > this.height - 2) {
            this.panelY = this.height / 2 + 12 - this.panelH;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        if (this.openDropdown != DROPDOWN_NONE) {
            renderDropdown(graphics, mouseX, mouseY);
        }
    }

    private void renderDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        layoutPanel();
        List<Entry> list = entries();
        graphics.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, 0xF0202020);
        for (int i = 0; i < list.size(); i++) {
            int col = i % this.panelCols;
            int row = i / this.panelCols;
            int x = this.panelX + 1 + col * 62;
            int y = this.panelY + 1 + row * 12;
            boolean hover = mouseX >= x && mouseX < x + 61 && mouseY >= y && mouseY < y + 12;
            if (hover) {
                graphics.fill(x, y, x + 61, y + 12, 0xFF3A3A5A);
            }
            Entry e = list.get(i);
            if (e.swatch() == -1) {
                // 原始贴图示意：边框 + 填充
                graphics.fill(x + 2, y + 2, x + 10, y + 10, 0xFFB08C5E);
                graphics.fill(x + 4, y + 4, x + 8, y + 8, 0xFFE9D7B7);
            } else {
                graphics.fill(x + 2, y + 2, x + 10, y + 10, 0xFF000000 | e.swatch());
            }
            graphics.drawString(this.font, e.label(), x + 13, y + 2, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.openDropdown != DROPDOWN_NONE) {
            List<Entry> list = entries();
            for (int i = 0; i < list.size(); i++) {
                int col = i % this.panelCols;
                int row = i / this.panelCols;
                int x = this.panelX + 1 + col * 62;
                int y = this.panelY + 1 + row * 12;
                if (mouseX >= x && mouseX < x + 61 && mouseY >= y && mouseY < y + 12) {
                    apply(list.get(i));
                    this.openDropdown = DROPDOWN_NONE;
                    return true;
                }
            }
            // 点面板外：收起并吞掉这次点击
            this.openDropdown = DROPDOWN_NONE;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void apply(Entry entry) {
        if (this.openDropdown == DROPDOWN_TEXT) {
            this.curTextColor = entry.value();
        } else if (this.openDropdown == DROPDOWN_BOARD) {
            this.curBoardColor = entry.value();
        } else if (this.openDropdown == DROPDOWN_FONT) {
            this.curFontId = FONTS[entry.value()][1];
        }
        // 整包发送完整样式状态
        BigSignNetwork.CHANNEL.sendToServer(
                new BigSignNetwork.SetStylePacket(this.pos, this.curTextColor, this.curBoardColor, this.curFontId));
        this.textColorBtn.setMessage(textColorLabel());
        this.boardColorBtn.setMessage(boardColorLabel());
        this.fontBtn.setMessage(fontLabel());
    }

    @Override
    public void tick() {
        this.input.tick();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.submit();
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        this.submit();
        super.onClose();
    }

    private void submit() {
        if (this.submitted || this.minecraft == null || this.minecraft.getConnection() == null) {
            return;
        }
        this.submitted = true;

        String value = this.input.getValue();
        if (value.length() > 1) {
            value = value.substring(0, 1);
        }
        BigSignNetwork.CHANNEL.sendToServer(new BigSignNetwork.SetTextPacket(this.pos, value));
    }
}
