package io.github.myworldzycpc.material_preparer.client.config;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.ListOptionEntry;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ControllerWidget;
import dev.isxander.yacl3.gui.utils.GuiUtils;
import io.github.myworldzycpc.material_preparer.client.screen.EditItemScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import static io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient.mc;

public class ItemEntryController implements Controller<ItemEntry> {
    private final ListOptionEntry<ItemEntry> option;

    public ItemEntryController(ListOptionEntry<ItemEntry> option) {
        this.option = option;
    }

    @Override
    public Option<ItemEntry> option() {
        return option;
    }

    @Override
    public Component formatValue() {
        Item item = option.pendingValue().item();
        if (item == null) {
            return Component.translatable("gui.material_preparer.none");
        }
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        Component displayName = item.getDefaultInstance().getHoverName();
        return Component.translatable("%1$s (%2$s) x %3$s", itemId, displayName, option.pendingValue().count());
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new ItemEntryElement(this, screen, widgetDimension);
    }

    public static class ItemEntryElement extends SubActionElement<ItemEntryController> {

        public ItemEntryElement(ItemEntryController control, YACLScreen screen, Dimension<Integer> dim) {
            super(control, screen, dim);
        }

        @Override
        public boolean mouseClickedOnBody(double mouseX, double mouseY, int button) {
            if (!isMouseOver(mouseX, mouseY) || !isAvailable())
                return false;
            mc.setScreen(new EditItemScreen(mc.screen, control.option));
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected void drawValueText(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            super.drawValueText(graphics, mouseX, mouseY, delta);
        }

        public Component getSubActionText() {
            return Component.translatable("gui.material_preparer.item_entry.divide");
        }
    }
}
