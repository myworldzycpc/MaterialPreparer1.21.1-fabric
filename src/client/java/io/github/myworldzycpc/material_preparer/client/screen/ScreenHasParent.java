package io.github.myworldzycpc.material_preparer.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient.mc;

public class ScreenHasParent extends Screen {
    public Screen parent;

    protected ScreenHasParent(Screen parent, Component component) {
        super(component);
        this.parent = parent;
    }

    @Override
    public void onClose() {
        mc.setScreen(this.parent);
    }
}
