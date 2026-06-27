package io.github.myworldzycpc.material_preparer.client.keybind;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class KeybindEntry {
    public final CustomKeybind keybind = new CustomKeybind();
    public final Runnable action;
    public final Consumer<String> configSaver;  // val -> config.xxxKeybind = val
    public final Supplier<String> configLoader;  // config.xxxKeybind -> val

    public KeybindEntry(Runnable action, Consumer<String> configSaver, Supplier<String> configLoader) {
        this.action = action;
        this.configSaver = configSaver;
        this.configLoader = configLoader;
    }
}