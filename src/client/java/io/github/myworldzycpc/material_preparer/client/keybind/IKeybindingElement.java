package io.github.myworldzycpc.material_preparer.client.keybind;

import io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public interface IKeybindingElement {
    default boolean isCapturing() {
        return MaterialPreparerClient.capturingElement == this;
    }

    default void toggleCapturing() {
        if (isCapturing()) {
            MaterialPreparerClient.capturingElement.stopCapturing();
        } else {
            if (MaterialPreparerClient.capturingElement != null) MaterialPreparerClient.capturingElement.stopCapturing();
            MaterialPreparerClient.capturingElement = this;
        }
    }

    default void stopCapturing() {
        if (isCapturing()) {
            MaterialPreparerClient.capturingElement = null;
        }
    }

    default Component getKeybindText() {
        if (isCapturing()) return Component.literal("[...]").withStyle(ChatFormatting.YELLOW);
        return Component.translatable("[%1$s]", getKeybind().getDisplayName());
    }

    CustomKeybind getKeybind();

    static int keyCodeToMods(int glfwMods) {
        int mods = 0;
        if ((glfwMods & GLFW.GLFW_MOD_CONTROL) != 0) mods |= CustomKeybind.MOD_CTRL;
        if ((glfwMods & GLFW.GLFW_MOD_ALT) != 0) mods |= CustomKeybind.MOD_ALT;
        if ((glfwMods & GLFW.GLFW_MOD_SHIFT) != 0) mods |= CustomKeybind.MOD_SHIFT;
        return mods;
    }

    default void capture(int keyCode, int scanCode, int modifiers) {
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


}
