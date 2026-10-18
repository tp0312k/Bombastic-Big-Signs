package com.bigsign.client.screen;

import com.bigsign.network.BigSignNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class BigSignEditScreen extends Screen {

    private final BlockPos pos;
    private final String existing;
    private EditBox input;
    private boolean submitted;

    public BigSignEditScreen(BlockPos pos, String existing) {
        super(Component.translatable("bigsign.screen.title"));
        this.pos = pos;
        this.existing = existing == null ? "" : existing;
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
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        this.input.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
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