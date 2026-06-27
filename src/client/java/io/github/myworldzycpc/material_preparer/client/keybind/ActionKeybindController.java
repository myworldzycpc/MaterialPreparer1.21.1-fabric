package io.github.myworldzycpc.material_preparer.client.keybind;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;

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

    public static class ActionKeybindElement extends KeybindingElement<ActionKeybindController> {

        public ActionKeybindElement(ActionKeybindController control, YACLScreen screen, Dimension<Integer> dim) {
            super(control, screen, dim);
        }

        public AABB getExecBox() {
            Component execText = control.formatValue();
            int execWidth = textRenderer.width(execText);
            int rightEdge = getBodyDimension().xLimit();
            int execX = rightEdge - getXPadding() - execWidth;
            int execY = getTextY();
            return new AABB(execX, execY, execY, execX + execWidth, execY + textRenderer.lineHeight, 0);
        }

        @Override
        public boolean mouseClickedOnBody(@NonNull MouseButtonEvent event, boolean doubleClick) {
            control.action().run();
            playDownSound();
            return true;
        }

        @Override
        protected void extractValueText(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            Component execText = control.formatValue();
            AABB execBox = getExecBox();

            if (isBodyHovered(mouseX, mouseY)) {
                if (isHovered()) {
                    execText = execText.copy().withStyle(ChatFormatting.AQUA);
                }
                graphics.text(textRenderer, execText, (int) execBox.minX, (int) execBox.minY, -1, true);
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
            super.capture(keyCode, scanCode, modifiers);
            control.onKeybindChanged().accept(getKeybind());
        }
    }
}
