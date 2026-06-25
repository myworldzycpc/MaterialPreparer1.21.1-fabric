package io.github.myworldzycpc.material_preparer.client.keybind;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ControllerWidget;
import dev.isxander.yacl3.gui.controllers.TickBoxController;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

import java.util.Objects;
import java.util.function.Consumer;

public final class TickBoxKeybindController extends TickBoxController {
    private final CustomKeybind keybind;
    private final Consumer<CustomKeybind> onKeybindChanged;

    public TickBoxKeybindController(Option<Boolean> option, CustomKeybind keybind, Consumer<CustomKeybind> onKeybindChanged) {
        super(option);
        this.keybind = keybind;
        this.onKeybindChanged = onKeybindChanged;
    }

    @Override
    public Component formatValue() {
        return super.formatValue();
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new TickBoxKeybindElement(this, screen, widgetDimension);
    }

    public CustomKeybind keybind() {
        return keybind;
    }

    public Consumer<CustomKeybind> onKeybindChanged() {
        return onKeybindChanged;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TickBoxKeybindController) obj;
        return Objects.equals(this.option(), that.option()) &&
                Objects.equals(this.keybind, that.keybind) &&
                Objects.equals(this.onKeybindChanged, that.onKeybindChanged);
    }

    @Override
    public int hashCode() {
        return Objects.hash(option(), keybind, onKeybindChanged);
    }

    @Override
    public String toString() {
        return "TickBoxKeybindController[" +
                "option=" + option() + ", " +
                "keybind=" + keybind + ", " +
                "onKeybindChanged=" + onKeybindChanged + ']';
    }


    public static class TickBoxKeybindElement extends KeybindingElement<TickBoxKeybindController> {
        private static final int TICKBOX_SIZE = 10;

        public TickBoxKeybindElement(TickBoxKeybindController control, YACLScreen screen, Dimension<Integer> dim) {
            super(control, screen, dim);
        }

        @Override
        public boolean mouseClickedOnBody(double mouseX, double mouseY, int button) {
            toggleSetting();
            return true;
        }

        @Override
        protected void drawHoveredControl(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            int outlineSize = 10;

            int outlineX1 = getBodyDimension().xLimit() - getXPadding() - outlineSize;
            int outlineY1 = getBodyDimension().centerY() - outlineSize / 2;
            int outlineX2 = getBodyDimension().xLimit() - getXPadding();
            int outlineY2 = getBodyDimension().centerY() + outlineSize / 2;

            int color = isAvailable() ? isBodyHovered(mouseX, mouseY) ? Objects.requireNonNullElse(ChatFormatting.AQUA.getColor(), -1) | 0xFF000000 : getValueColor() : inactiveColor;

            int shadowColor = multiplyColor(color, 0.25f);

            drawOutline(graphics, outlineX1 + 1, outlineY1 + 1, outlineX2 + 1, outlineY2 + 1, 1, shadowColor);
            drawOutline(graphics, outlineX1, outlineY1, outlineX2, outlineY2, 1, color);
            if (control.option().pendingValue()) {
                graphics.fill(outlineX1 + 3, outlineY1 + 3, outlineX2 - 1, outlineY2 - 1, shadowColor);
                graphics.fill(outlineX1 + 2, outlineY1 + 2, outlineX2 - 2, outlineY2 - 2, color);
            }
        }

        @Override
        protected void drawValueText(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            if (!isHovered()) drawHoveredControl(graphics, mouseX, mouseY, delta);
        }

        @Override
        protected int getHoveredControlWidth() {
            return getUnhoveredControlWidth();
        }

        @Override
        protected int getUnhoveredControlWidth() {
            return TICKBOX_SIZE + 5 + textRenderer.width(getKeybindText()) + getXPadding();
        }

        @Override
        public boolean canReset() {
            return super.canReset();
        }

        @Override
        public CustomKeybind getKeybind() {
            return control.keybind();
        }

        private void toggleSetting() {
            control.option().requestSet(!control.option().pendingValue());
        }

        @Override
        public void capture(int keyCode, int scanCode, int modifiers) {
            super.capture(keyCode, scanCode, modifiers);
            control.onKeybindChanged().accept(getKeybind());
        }
    }
}
