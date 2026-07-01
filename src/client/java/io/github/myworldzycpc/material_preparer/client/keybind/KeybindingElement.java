package io.github.myworldzycpc.material_preparer.client.keybind;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.utils.GuiUtils;
import io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient;
import io.github.myworldzycpc.material_preparer.client.config.SubActionElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public abstract class KeybindingElement<T extends Controller<?>> extends SubActionElement<T> {

    public KeybindingElement(T control, YACLScreen screen, Dimension<Integer> dim) {
        super(control, screen, dim);
    }

    @Override
    public int getSubActionWidth() {
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

    @Override
    public Component getSubActionText() {
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

    @Override
    public boolean mouseClickedOnSubAction(double mouseX, double mouseY, int button) {
        toggleCapturing();
        return super.mouseClickedOnSubAction(mouseX, mouseY, button);
    }

    public Dimension<Integer> getBodyDimension() {
        return Dimension.ofInt(getDimension().x(), getDimension().y(), getDimension().width() - getSubActionWidth(), getDimension().height());
    }
}
