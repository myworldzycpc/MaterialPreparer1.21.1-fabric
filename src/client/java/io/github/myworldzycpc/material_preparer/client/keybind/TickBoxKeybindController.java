package io.github.myworldzycpc.material_preparer.client.keybind;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Consumer;

public record TickBoxKeybindController(Option<Boolean> option, CustomKeybind keybind, Consumer<CustomKeybind> onKeybindChanged) implements Controller<Boolean> {

    @Override
    public Component formatValue() {
        return Component.empty();
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new TickBoxKeybindElement(this, screen, widgetDimension);
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
    public @NonNull String toString() {
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
        public boolean mouseClickedOnBody(@NonNull MouseButtonEvent event, boolean doubleClick) {
            toggleSetting();
            return true;
        }

        @Override
        protected void extractHoveredControl(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            int outlineSize = 10;

            int outlineX1 = getBodyDimension().xLimit() - getXPadding() - outlineSize;
            int outlineY1 = getBodyDimension().centerY() - outlineSize / 2;
            int outlineX2 = getBodyDimension().xLimit() - getXPadding();
            int outlineY2 = getBodyDimension().centerY() + outlineSize / 2;

            int color = isAvailable() ? isBodyHovered(mouseX, mouseY) ? TextColor.AQUA.getValue() | 0xFF000000 : getValueColor() : inactiveColor;

            int shadowColor = multiplyColor(color, 0.25f);

            graphics.outline(outlineX1 + 1, outlineY1 + 1, outlineX2 + 1 - (outlineX1 + 1), outlineY2 + 1 - (outlineY1 + 1), shadowColor);
            graphics.outline(outlineX1, outlineY1, outlineX2 - outlineX1, outlineY2 - outlineY1, color);
            if (this.control.option().pendingValue()) {
                graphics.fill(outlineX1 + 3, outlineY1 + 3, outlineX2 - 1, outlineY2 - 1, shadowColor);
                graphics.fill(outlineX1 + 2, outlineY1 + 2, outlineX2 - 2, outlineY2 - 2, color);
            }

            if (this.hovered) {
                graphics.requestCursor(this.isAvailable() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
            }
        }

        @Override
        protected void extractValueText(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            if (!isHovered()) extractHoveredControl(graphics, mouseX, mouseY, delta);
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
