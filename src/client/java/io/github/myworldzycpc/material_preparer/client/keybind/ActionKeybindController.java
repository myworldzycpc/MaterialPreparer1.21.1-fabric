package io.github.myworldzycpc.material_preparer.client.keybind;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ControllerWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public record ActionKeybindController(Option<Component> option, CustomKeybind keybind, Runnable action, Consumer<CustomKeybind> onKeybindChanged) implements Controller<Component> {

    @Override
    public Component formatValue() {
        return Component.translatable("yacl.control.action.execute");
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new ActionKeybindElement(this, screen, widgetDimension);
    }

    public static class ActionKeybindElement extends ControllerWidget<ActionKeybindController> implements IKeybindingElement {

        public ActionKeybindElement(ActionKeybindController control, YACLScreen screen, Dimension<Integer> dim) {
            super(control, screen, dim);
        }

        public AABB getKeybindBox() {
            Component keybindText = getKeybindText();
            int keybindWidth = textRenderer.width(keybindText);
            int keybindX = (int) getExecBox().minX - 5 - keybindWidth;
            int keybindY = getTextY();
            return new AABB(keybindX, keybindY, keybindY, keybindX + keybindWidth, keybindY + textRenderer.lineHeight, 0);
        }

        public AABB getExecBox() {
            Component execText = control.formatValue();
            int execWidth = textRenderer.width(execText);
            int rightEdge = getDimension().xLimit();
            int execX = rightEdge - getXPadding() - execWidth;
            int execY = getTextY();
            return new AABB(execX, execY, execY, execX + execWidth, execY + textRenderer.lineHeight, 0);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isMouseOver(mouseX, mouseY) || !isAvailable()) return false;

            int rightEdge = getDimension().xLimit();
            AABB keybindBox = getKeybindBox();
            if (mouseX >= keybindBox.minX - 3 && mouseX <= keybindBox.maxX + 3) {
                toggleCapturing();
                playDownSound();
                return true;
            }

            control.action().run();
            playDownSound();
            return true;
        }

        @Override
        protected void drawValueText(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            int rightEdge = getDimension().xLimit();

            Component keybindText = getKeybindText();
            AABB keybindBox = getKeybindBox();

            Component execText = control.formatValue();
            AABB execBox = getExecBox();

            int baseColor = getValueColor();
            boolean keybindHovered = isHovered() && mouseX >= keybindBox.minX - 3 && mouseX <= keybindBox.maxX + 3;
            int color = ChatFormatting.AQUA.getColor() != null ? ChatFormatting.AQUA.getColor() : baseColor;
            if (isHovered()) {
                if (!keybindHovered) {
                    graphics.drawString(textRenderer, execText, (int) execBox.minX, (int) execBox.minY, color, true);
                    graphics.drawString(textRenderer, keybindText, (int) keybindBox.minX, (int) execBox.minY, baseColor, true);
                } else {
                    graphics.drawString(textRenderer, execText, (int) execBox.minX, (int) execBox.minY, baseColor, true);
                    graphics.drawString(textRenderer, keybindText, (int) keybindBox.minX, (int) execBox.minY, color, true);
                }
            } else {
                graphics.drawString(textRenderer, execText, (int) execBox.minX, (int) execBox.minY, baseColor, true);
                graphics.drawString(textRenderer, keybindText, (int) keybindBox.minX, (int) execBox.minY, baseColor, true);
            }
        }

        @Override
        protected int getHoveredControlWidth() {
            return getUnhoveredControlWidth();
        }

        @Override
        protected int getUnhoveredControlWidth() {
            Component execText = control.formatValue();
            Component keybindText = getKeybindText();
            return textRenderer.width(execText) + 5 + textRenderer.width(keybindText) + getXPadding();
        }

        @Override
        public boolean canReset() {
            return false;
        }

        @Override
        public CustomKeybind getKeybind() {
            return control.keybind();
        }

        @Override
        public void capture(int keyCode, int scanCode, int modifiers) {
            IKeybindingElement.super.capture(keyCode, scanCode, modifiers);
            control.onKeybindChanged().accept(getKeybind());
        }
    }
}
