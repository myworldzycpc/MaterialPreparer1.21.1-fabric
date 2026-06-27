package io.github.myworldzycpc.material_preparer.client.keybind;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ControllerWidget;
import dev.isxander.yacl3.gui.utils.GuiUtils;
import io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

public abstract class KeybindingElement<T extends Controller<?>> extends ControllerWidget<T> {

    public KeybindingElement(T control, YACLScreen screen, Dimension<Integer> dim) {
        super(control, screen, dim);
    }

    public int getKeybindWidth() {
        return 50;
    }

    public boolean isCapturing() {
        return MaterialPreparerClient.capturingElement == this;
    }

    public void toggleCapturing() {
        if (isCapturing()) {
            MaterialPreparerClient.capturingElement.stopCapturing();
        } else {
            if (MaterialPreparerClient.capturingElement != null) MaterialPreparerClient.capturingElement.stopCapturing();
            MaterialPreparerClient.capturingElement = this;
        }
    }

    public void stopCapturing() {
        if (isCapturing()) {
            MaterialPreparerClient.capturingElement = null;
        }
    }

    public Component getKeybindText() {
        if (isCapturing()) return Component.literal("...").withStyle(ChatFormatting.YELLOW);
        return getKeybind().getDisplayName();
    }

    abstract public CustomKeybind getKeybind();

    public static int keyCodeToMods(int glfwMods) {
        int mods = 0;
        if ((glfwMods & GLFW.GLFW_MOD_CONTROL) != 0) mods |= CustomKeybind.MOD_CTRL;
        if ((glfwMods & GLFW.GLFW_MOD_ALT) != 0) mods |= CustomKeybind.MOD_ALT;
        if ((glfwMods & GLFW.GLFW_MOD_SHIFT) != 0) mods |= CustomKeybind.MOD_SHIFT;
        return mods;
    }

    public void capture(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            getKeybind().setKeyCode(-1);
            getKeybind().setModifiers(0);
        } else {
            getKeybind().setKeyCode(keyCode);
//            getKeybind().setModifiers(keyCodeToMods(modifiers));
            getKeybind().setModifiers(0);
        }
        stopCapturing();
    }

    public boolean isBodyHovered(int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY) || !isAvailable()) return false;
        return !isKeybindHovered(mouseX, mouseY);
    }

    public boolean isKeybindHovered(int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY) || !isAvailable()) return false;
        return mouseX >= getDimension().xLimit() - getKeybindWidth();
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        int button = event.button();
        double mouseX = event.x();
        double mouseY = event.y();
        if (!isMouseOver(mouseX, mouseY) || !isAvailable()) return false;
        if (mouseX >= getDimension().xLimit() - getKeybindWidth()) {
            toggleCapturing();
            playDownSound();
            return true;
        }
        return mouseClickedOnBody(event, doubleClick);
    }

    public boolean mouseClickedOnBody(@NonNull MouseButtonEvent event, boolean doubleClick) {
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        hovered = isMouseOver(mouseX, mouseY);

        Component name = control.option().changed() ? modifiedOptionName : control.option().name();
        Component shortenedName = Component.literal(GuiUtils.shortenString(name.getString(), textRenderer, getDimension().width() - getControlWidth() - getXPadding() - 7, "...")).setStyle(name.getStyle());

        drawButtonRect(graphics, getDimension().x(), getDimension().y(), getDimension().xLimit() - getKeybindWidth(), getDimension().yLimit(), isBodyHovered(mouseX, mouseY), isAvailable());
        drawButtonRect(graphics, getDimension().xLimit() - getKeybindWidth(), getDimension().y(), getDimension().xLimit(), getDimension().yLimit(), isKeybindHovered(mouseX, mouseY), isAvailable());
        graphics.text(textRenderer, shortenedName, getDimension().x() + getXPadding(), getTextY(), getValueColor(), true);

        extractValueText(graphics, mouseX, mouseY, delta);
        if (isHovered()) {
            extractHoveredControl(graphics, mouseX, mouseY, delta);
        }

        graphics.centeredText(textRenderer, getKeybindText(), getDimension().xLimit() - getKeybindWidth() / 2, getTextY(), -1);
    }

    public Dimension<Integer> getBodyDimension() {
        return Dimension.ofInt(getDimension().x(), getDimension().y(), getDimension().width() - getKeybindWidth(), getDimension().height());
    }
}
