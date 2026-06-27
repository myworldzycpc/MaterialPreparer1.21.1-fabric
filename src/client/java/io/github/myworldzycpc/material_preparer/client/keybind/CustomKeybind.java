package io.github.myworldzycpc.material_preparer.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

public class CustomKeybind {
    public static final int MOD_CTRL = 1;
    public static final int MOD_ALT = 2;
    public static final int MOD_SHIFT = 4;

    private int keyCode;
    private int modifiers;

    public CustomKeybind() {
        this(-1, 0);
    }

    public CustomKeybind(int keyCode, int modifiers) {
        this.keyCode = keyCode;
        this.modifiers = modifiers & (MOD_CTRL | MOD_ALT | MOD_SHIFT);
    }

    public boolean isUnbound() {
        return keyCode < 0;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public int getModifiers() {
        return modifiers;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public void setModifiers(int modifiers) {
        this.modifiers = modifiers & (MOD_CTRL | MOD_ALT | MOD_SHIFT);
    }

    public boolean matches(int pressedKeyCode, int glfwMods) {
        if (isUnbound()) return false;
        if (keyCode != pressedKeyCode) return false;
        int mods = 0;
        if ((glfwMods & GLFW.GLFW_MOD_CONTROL) != 0) mods |= MOD_CTRL;
        if ((glfwMods & GLFW.GLFW_MOD_ALT) != 0) mods |= MOD_ALT;
        if ((glfwMods & GLFW.GLFW_MOD_SHIFT) != 0) mods |= MOD_SHIFT;
        return modifiers == mods;
    }

    public Component getDisplayName() {
        if (isUnbound()) return Component.translatable("key.keyboard.unknown");
        StringBuilder sb = new StringBuilder();
        if ((modifiers & MOD_CTRL) != 0) sb.append("Ctrl+");
        if ((modifiers & MOD_ALT) != 0) sb.append("Alt+");
        if ((modifiers & MOD_SHIFT) != 0) sb.append("Shift+");
        String keyName = GLFW.glfwGetKeyName(keyCode, 0);
        if (keyName != null && !keyName.isEmpty()) {
            sb.append(keyName.toUpperCase());
        } else {
            sb.append(InputConstants.getKey(new KeyEvent(keyCode, 0, modifiers)).getDisplayName().getString());
        }
        return Component.literal(sb.toString());
    }

    public String serialize() {
        if (isUnbound()) return "unbound";
        return keyCode + ":" + modifiers;
    }

    public static CustomKeybind deserialize(String str) {
        if (str == null || str.equals("unbound")) return new CustomKeybind();
        String[] parts = str.split(":");
        try {
            int keyCode = Integer.parseInt(parts[0]);
            int modifiers = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return new CustomKeybind(keyCode, modifiers);
        } catch (NumberFormatException e) {
            return new CustomKeybind();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomKeybind that)) return false;
        return keyCode == that.keyCode && modifiers == that.modifiers;
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyCode, modifiers);
    }

    public void copyTo(CustomKeybind keybind) {
        keybind.keyCode = this.keyCode;
        keybind.modifiers = this.modifiers;
    }
}
