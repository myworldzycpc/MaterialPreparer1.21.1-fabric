package io.github.myworldzycpc.material_preparer.client.keybind;

import dev.isxander.yacl3.api.Controller;
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
import org.lwjgl.glfw.GLFW;

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


    public static class TickBoxKeybindElement extends ControllerWidget<TickBoxKeybindController> implements IKeybindingElement {
        private static final int TICKBOX_SIZE = 10;

        public TickBoxKeybindElement(TickBoxKeybindController control, YACLScreen screen, Dimension<Integer> dim) {
            super(control, screen, dim);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isMouseOver(mouseX, mouseY) || !isAvailable()) return false;
            AABB keybindBox = getKeybindBox();

            if (mouseX >= keybindBox.minX - 3 && mouseX <= keybindBox.maxX + 3) {
                toggleCapturing();
                playDownSound();
                return true;
            }

            toggleSetting();
            return true;
        }

        @Override
        protected void drawHoveredControl(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            int outlineSize = 10;

            int outlineX1 = getDimension().xLimit() - getXPadding() - outlineSize;
            int outlineY1 = getDimension().centerY() - outlineSize / 2;
            int outlineX2 = getDimension().xLimit() - getXPadding();
            int outlineY2 = getDimension().centerY() + outlineSize / 2;

            int color = getValueColor();
            int shadowColor = multiplyColor(color, 0.25f);

            drawOutline(graphics, outlineX1 + 1, outlineY1 + 1, outlineX2 + 1, outlineY2 + 1, 1, shadowColor);
            drawOutline(graphics, outlineX1, outlineY1, outlineX2, outlineY2, 1, color);
            if (control.option().pendingValue()) {
                graphics.fill(outlineX1 + 3, outlineY1 + 3, outlineX2 - 1, outlineY2 - 1, shadowColor);
                graphics.fill(outlineX1 + 2, outlineY1 + 2, outlineX2 - 2, outlineY2 - 2, color);
            }
        }

        public AABB getKeybindBox() {
            Component keybindText = getKeybindText();
            int rightEdge = getDimension().xLimit();
            int keybindWidth = textRenderer.width(keybindText);
            int keybindX = rightEdge - keybindWidth - getXPadding() - TICKBOX_SIZE - 5;
            int keybindY = getTextY();
            return new AABB(keybindX, keybindY, keybindY, keybindX + keybindWidth, keybindY + textRenderer.lineHeight, 0);
        }

        @Override
        protected void drawValueText(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            if (!isHovered())
                drawHoveredControl(graphics, mouseX, mouseY, delta);

            Component keybindText = getKeybindText();
            AABB keybindBox = getKeybindBox();
            boolean isKeybindHovered = isHovered() && mouseX >= keybindBox.minX - 3 && mouseX <= keybindBox.maxX + 3;

            int textColor = getValueColor();
            if (isKeybindHovered) {
                Integer aquaColor = ChatFormatting.AQUA.getColor();
                if (aquaColor != null) textColor = aquaColor;
            }
            graphics.drawString(textRenderer, keybindText, (int) keybindBox.minX, (int) keybindBox.minY, textColor, true);
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
    }
}
